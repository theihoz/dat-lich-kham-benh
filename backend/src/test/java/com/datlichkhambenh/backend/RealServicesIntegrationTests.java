package com.datlichkhambenh.backend;

import com.datlichkhambenh.backend.dto.request.CreateBookingDto;
import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.messaging.NotificationDeliveryService;
import com.datlichkhambenh.backend.repository.*;
import com.datlichkhambenh.backend.security.AuthSessionService;
import com.datlichkhambenh.backend.service.BookingService;
import com.datlichkhambenh.backend.service.RedisCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named="CLINIC_REAL_SERVICES",matches="true")
class RealServicesIntegrationTests {
 @Autowired JdbcTemplate jdbc; @Autowired RedisCacheService cache;
 @Autowired PlatformTransactionManager transactions; @Autowired UserRepository users;
 @Autowired DoctorRepository doctors; @Autowired PatientRepository patients; @Autowired SpecialtyRepository specialties;
 @Autowired ScheduleRepository schedules; @Autowired BookingService booking;
 @Autowired NotificationDeliveryService delivery; @Autowired NotificationOutboxRepository outbox;
 @Autowired NotificationReceiptRepository receipts; @Autowired AuthSessionService sessions;
 @Autowired ObjectMapper json;
 @Autowired io.micrometer.core.instrument.MeterRegistry metrics;
 @Autowired com.datlichkhambenh.backend.service.AuthService auth;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
 @Autowired com.datlichkhambenh.backend.service.query.DoctorSearchQueryService doctorSearch;
 @Autowired org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;
 @Autowired org.springframework.amqp.core.AmqpAdmin rabbitAdmin;
 String suffix=UUID.randomUUID().toString();
 @Test void flywayMigratesAndHibernateValidatesRealMySql() {
  assertEquals(3,jdbc.queryForObject("select count(*) from flyway_schema_history where success=1 and version is not null",Integer.class));
 }
 @Test void realRedisRoundTripsTypedDoctorData() {
  long key=Math.abs(UUID.randomUUID().getMostSignificantBits());
  cache.cacheDoctors(key,List.of(DoctorDto.builder().userId(key).fullName("Real cache "+suffix).consultationFee(BigDecimal.TEN).build()));
  assertEquals("Real cache "+suffix,cache.getCachedDoctors(key).getFirst().getFullName());cache.evictDoctorCache(key);
 }
 @Test void transactionalOutboxPublishesConfirmedRabbitMessageAndDeliversSmtp() throws Exception {
  String email="delivery-"+suffix+"@example.test";
  new TransactionTemplate(transactions).executeWithoutResult(tx->delivery.enqueueAccountMessage("PASSWORD_RESET",email,"fake-test-token"));
  await(()->outbox.findAll().stream().filter(o->o.getPayload().contains(email)).anyMatch(o->"SENT".equals(o.getStatus())),15000);
  var item=outbox.findAll().stream().filter(o->o.getPayload().contains(email)).findFirst().orElseThrow();
  await(()->receipts.existsById(item.getId()),15000);
  var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:18026/api/v1/search?query="+email)).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,response.statusCode()); assertTrue(json.readTree(response.body()).path("total").asInt()>0,"Mailpit must receive real SMTP mail");
 }
 @Test void concurrentMySqlBookingsHaveOneWinner() throws Exception {
  var tx=new TransactionTemplate(transactions);
  long[] ids=tx.execute(t->{
   Specialty specialty=specialties.save(Specialty.builder().name("Integration "+suffix).build());
   User doctorUser=users.saveAndFlush(User.builder().email("doctor-"+suffix+"@example.test").passwordHash("unused").role("DOCTOR").isActive(true).build());
   Doctor doctor=doctors.saveAndFlush(Doctor.builder().user(doctorUser).specialty(specialty).fullName("Test doctor").isApproved(true).consultationFee(BigDecimal.TEN).build());
   User patientUser=users.saveAndFlush(User.builder().email("patient-"+suffix+"@example.test").passwordHash("unused").role("PATIENT").isActive(true).build());
   Patient patient=patients.saveAndFlush(Patient.builder().user(patientUser).fullName("Test patient").build());
   Schedule slot=schedules.saveAndFlush(Schedule.builder().doctor(doctor).workDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(10,0)).endTime(LocalTime.of(10,30)).isAvailable(true).build());
   return new long[]{patient.getUserId(),slot.getId()};
  });
  ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
  try {
   Callable<Boolean> attempt=()->{gate.await();try{booking.createBooking(ids[0],CreateBookingDto.builder().scheduleId(ids[1]).symptoms("Integration contention").build());return true;}catch(RuntimeException e){return false;}};
   Future<Boolean> first=pool.submit(attempt),second=pool.submit(attempt);gate.countDown();
   assertEquals(1,(first.get(15,TimeUnit.SECONDS)?1:0)+(second.get(15,TimeUnit.SECONDS)?1:0));
   assertEquals(1,jdbc.queryForObject("select count(*) from appointments where schedule_id=?",Integer.class,ids[1]));
  } finally {pool.shutdownNow();}
 }
 @Test void concurrentRefreshRotationSerializesAndReplayRevokesFamilyOnMySql() throws Exception {
  User user=new TransactionTemplate(transactions).execute(tx->users.saveAndFlush(User.builder().email("session-"+suffix+"@example.test").passwordHash("unused").role("PATIENT").isActive(true).build()));
  var login=sessions.issue(user); ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch gate=new CountDownLatch(1);
  try {
   Callable<Boolean> attempt=()->{gate.await();try{sessions.rotate(login.getRefreshToken());return true;}catch(RuntimeException ex){return false;}};
   var a=pool.submit(attempt);var b=pool.submit(attempt);gate.countDown();
   assertEquals(1,(a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0));
   assertEquals(0,jdbc.queryForObject("select count(*) from auth_refresh_sessions where user_id=? and revoked_at is null",Integer.class,user.getId()));
  } finally {pool.shutdownNow();}
 }
 @Test void databaseDoctorSearchUsesLiteralKeywordAndBoundedPages() {
  var tx=new TransactionTemplate(transactions);
  String keyword="%_"+suffix;
  tx.executeWithoutResult(t->{
   var specialty=specialties.save(Specialty.builder().name("Paging "+suffix).build());
   for(int i=0;i<3;i++) {
    User user=users.saveAndFlush(User.builder().email("search"+i+suffix+"@example.test").passwordHash("unused").role("DOCTOR").isActive(true).build());
    doctors.saveAndFlush(Doctor.builder().user(user).specialty(specialty).fullName(keyword+i).isApproved(true).consultationFee(BigDecimal.valueOf(10+i)).build());
   }
  });
  var first=doctorSearch.search(null,keyword,BigDecimal.TEN,null,null,"feeAsc",0,2);
  var second=doctorSearch.search(null,keyword,BigDecimal.TEN,null,null,"feeAsc",1,2);
  assertEquals(2,first.size());assertEquals(1,second.size());assertEquals(BigDecimal.valueOf(12).setScale(2),second.getFirst().getConsultationFee());
 }
 @Test void failedDeliveryRetriesAreBoundedAndReachTheDeadLetterQueue() throws Exception {
  String queue="clinic.email.queue.dlq";
  var before=rabbitAdmin.getQueueProperties(queue);
  int initial=before==null?0:((Number)before.get("QUEUE_MESSAGE_COUNT")).intValue();
  String id="invalid-"+suffix;
  var props=new org.springframework.amqp.core.MessageProperties();props.setMessageId(id);props.setHeader("notificationKind","UNSUPPORTED");
  rabbit.send("clinic.email.exchange","clinic.email.send",new org.springframework.amqp.core.Message("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),props));
  await(()->{var p=rabbitAdmin.getQueueProperties(queue);return p!=null && ((Number)p.get("QUEUE_MESSAGE_COUNT")).intValue()>initial;},15000);
  assertFalse(receipts.existsById(id),"Failed SMTP work must not leave a committed receipt");
  var deadLetters=metrics.find("clinic.notifications.consumer.dead").tag("queue",queue).gauge();
  assertNotNull(deadLetters,"Broker dead letters must be visible to monitoring");
  assertTrue(deadLetters.value()>initial);

 }
 @Test void loginCannotIssueSessionUsingPasswordChangedWhileWaitingForUserLock() throws Exception {
  var tx=new TransactionTemplate(transactions);
  User user=tx.execute(t->users.saveAndFlush(User.builder().email("login-race-"+suffix+"@example.test").passwordHash(passwords.encode("OldPassword123")).role("PATIENT").isActive(true).build()));
  var pool=Executors.newSingleThreadExecutor();var started=new CountDownLatch(1);
  var attempt=new java.util.concurrent.atomic.AtomicReference<Future<Boolean>>();
  try {
   tx.executeWithoutResult(t->{
    User locked=users.findAuthUserForUpdate(user.getId()).orElseThrow();
    attempt.set(pool.submit(()->{started.countDown();try{auth.login(new com.datlichkhambenh.backend.dto.request.LoginRequestDto(user.getEmail(),"OldPassword123"));return true;}catch(RuntimeException rejected){return false;}}));
    try {assertTrue(started.await(2,TimeUnit.SECONDS));try{attempt.get().get(600,TimeUnit.MILLISECONDS);}catch(TimeoutException expected){}catch(ExecutionException failure){throw new RuntimeException(failure);}}catch(InterruptedException e){throw new RuntimeException(e);}
    locked.setPasswordHash(passwords.encode("ChangedPassword123"));locked.setCredentialsVersion(locked.getCredentialsVersion()+1);users.saveAndFlush(locked);
   });
   assertFalse(attempt.get().get(10,TimeUnit.SECONDS),"An old password must not receive a fresh session after credential change commits");
  } finally {pool.shutdownNow();}
 }
 private void await(BooleanSupplier condition,long timeout) throws InterruptedException {
  long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeout);
  while(!condition.getAsBoolean() && System.nanoTime()<end) Thread.sleep(100);
  assertTrue(condition.getAsBoolean(),"External service condition did not become true");
 }
}
