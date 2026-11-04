package com.datlichkhambenh.backend.service.query;

import com.datlichkhambenh.backend.dto.response.*;
import com.datlichkhambenh.backend.entity.*;
import com.datlichkhambenh.backend.exception.AppException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminReadService {
 private final EntityManager em;
 public record Result<T>(List<T> content,long total) {}
 private int offset(int page,int size) {
  if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw new AppException("Trang không hợp lệ; size từ 1 đến 100");
  return page*size;
 }
 public Result<UserDto> users(String role,int page,int size) {
  int offset=offset(page,size); String where=role==null || role.isBlank()?"":" where u.role=:role";
  TypedQuery<User> query=em.createQuery("select u from User u"+where+" order by u.id",User.class);
  TypedQuery<Long> count=em.createQuery("select count(u) from User u"+where,Long.class);
  if(!where.isEmpty()){query.setParameter("role",role.toUpperCase(Locale.ROOT));count.setParameter("role",role.toUpperCase(Locale.ROOT));}
  return new Result<>(query.setFirstResult(offset).setMaxResults(size).getResultList().stream().map(u->UserDto.builder().id(u.getId()).email(u.getEmail()).role(u.getRole()).isActive(u.getIsActive()).createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build()).toList(),count.getSingleResult());
 }
 public Result<DoctorDto> doctors(int page,int size) {
  int offset=offset(page,size);
  return new Result<>(em.createQuery("select d from Doctor d join fetch d.user join fetch d.specialty order by d.userId",Doctor.class).setFirstResult(offset).setMaxResults(size).getResultList().stream().map(d->DoctorDto.builder().userId(d.getUserId()).email(d.getUser().getEmail()).isActive(d.getUser().getIsActive()).fullName(d.getFullName()).phone(d.getPhone()).degree(d.getDegree()).experienceYears(d.getExperienceYears()).consultationFee(d.getConsultationFee()).isApproved(d.getIsApproved()).specialtyId(d.getSpecialty().getId()).specialtyName(d.getSpecialty().getName()).build()).toList(),em.createQuery("select count(d) from Doctor d",Long.class).getSingleResult());
 }
 public Result<PatientDto> patients(int page,int size) {
  int offset=offset(page,size);
  return new Result<>(em.createQuery("select p from Patient p join fetch p.user order by p.userId",Patient.class).setFirstResult(offset).setMaxResults(size).getResultList().stream().map(p->PatientDto.builder().userId(p.getUserId()).email(p.getUser().getEmail()).isActive(p.getUser().getIsActive()).fullName(p.getFullName()).phone(p.getPhone()).dob(p.getDob()).gender(p.getGender()).address(p.getAddress()).medicalHistory(p.getMedicalHistory()).build()).toList(),em.createQuery("select count(p) from Patient p",Long.class).getSingleResult());
 }
 public Result<AppointmentDto> appointments(Long doctorId,int page,int size) {
  int offset=offset(page,size); String where=doctorId==null?"":" where a.doctor.userId=:doctorId";
  TypedQuery<Appointment> query=em.createQuery("select a from Appointment a join fetch a.patient join fetch a.doctor join fetch a.schedule"+where+" order by a.schedule.workDate desc,a.schedule.startTime desc,a.id desc",Appointment.class);
  TypedQuery<Long> count=em.createQuery("select count(a) from Appointment a"+where,Long.class);
  if(doctorId!=null){query.setParameter("doctorId",doctorId);count.setParameter("doctorId",doctorId);}
  return new Result<>(query.setFirstResult(offset).setMaxResults(size).getResultList().stream().map(this::appointment).toList(),count.getSingleResult());
 }
 private AppointmentDto appointment(Appointment a) {return AppointmentDto.builder().id(a.getId()).bookingCode(a.getBookingCode()).doctorId(a.getDoctor().getUserId()).doctorName(a.getDoctor().getFullName()).patientId(a.getPatient().getUserId()).patientName(a.getPatient().getFullName()).patientPhone(a.getPatient().getPhone()).scheduleId(a.getSchedule().getId()).workDate(a.getSchedule().getWorkDate()).startTime(a.getSchedule().getStartTime()).endTime(a.getSchedule().getEndTime()).symptoms(a.getSymptoms()).status(a.getStatus()).price(a.getPrice()).createdAt(a.getCreatedAt()).build();}
 private long count(String entity) {return em.createQuery("select count(e) from "+entity+" e",Long.class).getSingleResult();}
 public Map<String,Object> stats() {
  long total=count("Appointment");long cancelled=em.createQuery("select count(a) from Appointment a where a.status='CANCELLED'",Long.class).getSingleResult();
  Map<String,Object> data=new LinkedHashMap<>(); data.put("totalDoctors",count("Doctor"));data.put("totalPatients",count("Patient"));data.put("totalSpecialties",count("Specialty"));data.put("totalAppointments",total);data.put("cancelledAppointments",cancelled);data.put("cancellationRate",total==0?0:Math.round(cancelled*10000.0/total)/100.0);
  data.put("totalRevenue",em.createQuery("select coalesce(sum(a.price),0) from Appointment a where a.status='COMPLETED'",BigDecimal.class).getSingleResult());
  data.put("topDoctors",em.createQuery("select a.doctor.fullName,count(a) from Appointment a where a.status<>'CANCELLED' group by a.doctor.userId,a.doctor.fullName order by count(a) desc",Object[].class).setMaxResults(5).getResultList().stream().map(r->Map.of("doctorName",r[0],"appointments",r[1])).toList());return data;
 }
 public Map<String,Object> analytics(int year) {
  if(year<2000 || year>2200) throw new AppException("Năm thống kê không hợp lệ");
  LocalDate from=LocalDate.of(year,1,1),to=from.plusYears(1);
  String period=" a.schedule.workDate>=:from and a.schedule.workDate<:to";
  List<Object[]> rows=period("select month(a.schedule.workDate),count(a),sum(case when a.status='COMPLETED' then 1 else 0 end),sum(case when a.status='CANCELLED' then 1 else 0 end),sum(case when a.status='COMPLETED' then coalesce(a.price,0) else 0 end),sum(case when a.status='COMPLETED' and a.price is null then 1 else 0 end) from Appointment a where"+period+" group by month(a.schedule.workDate)",from,to).getResultList();
  Map<Integer,Object[]> byMonth=new HashMap<>();for(Object[] row:rows)byMonth.put(((Number)row[0]).intValue(),row);
  List<Map<String,Object>> months=new ArrayList<>();long total=0,completed=0,cancelled=0,missing=0;BigDecimal earned=BigDecimal.ZERO,previous=BigDecimal.ZERO;
  for(int month=1;month<=12;month++) {Object[] row=byMonth.get(month);long n=row==null?0:((Number)row[1]).longValue(),c=row==null?0:((Number)row[2]).longValue(),x=row==null?0:((Number)row[3]).longValue();BigDecimal fee=row==null?BigDecimal.ZERO:new BigDecimal(row[4].toString());Map<String,Object> m=new LinkedHashMap<>();m.put("month",month);m.put("appointments",n);m.put("completed",c);m.put("cancelled",x);m.put("revenue",fee);m.put("growthPercent",previous.signum()>0?fee.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous,1,RoundingMode.HALF_UP):null);months.add(m);total+=n;completed+=c;cancelled+=x;earned=earned.add(fee);previous=fee;if(row!=null)missing+=((Number)row[5]).longValue();}
  List<Map<String,Object>> daily=period("select a.schedule.workDate,sum(a.price) from Appointment a where"+period+" and a.status='COMPLETED' and a.price is not null group by a.schedule.workDate order by a.schedule.workDate",from,to).getResultList().stream().map(r->Map.<String,Object>of("date",r[0],"revenue",r[1])).toList();
  List<Map<String,Object>> rankings=period("select d.userId,d.fullName,s.name,count(a),sum(case when a.status='CONFIRMED' then 1 else 0 end),sum(case when a.status='COMPLETED' then 1 else 0 end),sum(case when a.status='CANCELLED' then 1 else 0 end),sum(case when a.status='COMPLETED' then coalesce(a.price,0) else 0 end) from Appointment a join a.doctor d join d.specialty s where"+period+" group by d.userId,d.fullName,s.name order by sum(case when a.status='COMPLETED' then coalesce(a.price,0) else 0 end) desc",from,to).setMaxResults(100).getResultList().stream().map(r->Map.<String,Object>of("doctorId",r[0],"doctorName",r[1],"specialty",r[2],"appointments",r[3],"confirmed",r[4],"completed",r[5],"cancelled",r[6],"revenue",r[7])).toList();
  List<Appointment> recent=em.createQuery("select a from Appointment a join fetch a.patient join fetch a.doctor d join fetch d.specialty join fetch a.schedule where"+period+" and a.status='COMPLETED' order by a.schedule.workDate desc,a.id desc",Appointment.class).setParameter("from",from).setParameter("to",to).setMaxResults(100).getResultList();
  List<Map<String,Object>> entries=recent.stream().map(a->{Map<String,Object> r=new LinkedHashMap<>();r.put("appointmentId",a.getId());r.put("bookingCode",a.getBookingCode());r.put("patientName",a.getPatient().getFullName());r.put("patientPhone",a.getPatient().getPhone());r.put("doctorName",a.getDoctor().getFullName());r.put("specialty",a.getDoctor().getSpecialty().getName());r.put("amount",a.getPrice());r.put("workDate",a.getSchedule().getWorkDate());r.put("startTime",a.getSchedule().getStartTime());r.put("month",a.getSchedule().getWorkDate().getMonthValue());r.put("status",a.getStatus());return r;}).toList();
  Map<String,Object> data=new LinkedHashMap<>();data.put("year",year);data.put("months",months);data.put("doctorRankings",rankings);data.put("statusCounts",Map.of("CONFIRMED",total-completed-cancelled,"COMPLETED",completed,"CANCELLED",cancelled));data.put("dailyRevenue",daily);data.put("transactions",entries);data.put("totalTransactions",completed);data.put("transactionsLimited",completed>100);data.put("yearAppointments",total);data.put("yearCompletedAppointments",completed);data.put("yearRevenue",earned);data.put("avgPerAppointment",completed==0?BigDecimal.ZERO:earned.divide(BigDecimal.valueOf(completed),2,RoundingMode.HALF_UP));data.put("revenueBasis","COMPLETED_APPOINTMENT_FEES");data.put("paymentVerified",false);data.put("completedWithoutPrice",missing);return data;
 }
 private TypedQuery<Object[]> period(String jpql,LocalDate from,LocalDate to){return em.createQuery(jpql,Object[].class).setParameter("from",from).setParameter("to",to);}
}
