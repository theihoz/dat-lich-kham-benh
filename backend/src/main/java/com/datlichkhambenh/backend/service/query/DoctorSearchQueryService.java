package com.datlichkhambenh.backend.service.query;

import com.datlichkhambenh.backend.dto.response.DoctorDto;
import com.datlichkhambenh.backend.entity.Doctor;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Bounded DB search for production; never loads the full directory to paginate it in Java. */
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DoctorSearchQueryService {
 private final EntityManager em;
 public List<DoctorDto> search(Long specialtyId,String keyword,BigDecimal minFee,BigDecimal maxFee,Integer experience,String sort,int page,int size) {
  StringBuilder query=new StringBuilder("select d from Doctor d join fetch d.user u join fetch d.specialty s where d.isApproved=true and u.isActive=true");
  Map<String,Object> args=new HashMap<>();
  if(specialtyId!=null){query.append(" and s.id=:specialty");args.put("specialty",specialtyId);}
  if(keyword!=null&&!keyword.isBlank()){query.append(" and locate(:keyword,lower(d.fullName))>0");args.put("keyword",keyword.trim().toLowerCase(Locale.ROOT));}
  if(minFee!=null){query.append(" and d.consultationFee>=:minFee");args.put("minFee",minFee);}
  if(maxFee!=null){query.append(" and d.consultationFee<=:maxFee");args.put("maxFee",maxFee);}
  if(experience!=null){query.append(" and d.experienceYears>=:experience");args.put("experience",experience);}
  query.append(switch(sort==null?"name":sort){case "feeAsc"->" order by d.consultationFee asc nulls last,d.fullName,d.userId";case "feeDesc"->" order by d.consultationFee desc nulls last,d.fullName,d.userId";case "experience"->" order by d.experienceYears desc nulls last,d.fullName,d.userId";default->" order by d.fullName,d.userId";});
  int limit=Math.min(100,Math.max(1,size));long offset=(long)Math.max(0,page)*limit;if(offset>Integer.MAX_VALUE)return List.of();
  var request=em.createQuery(query.toString(),Doctor.class);args.forEach(request::setParameter);
  return request.setFirstResult((int)offset).setMaxResults(limit).getResultList().stream().map(d->DoctorDto.builder().userId(d.getUserId()).email(d.getUser().getEmail()).fullName(d.getFullName()).phone(d.getPhone()).degree(d.getDegree()).experienceYears(d.getExperienceYears()).consultationFee(d.getConsultationFee()).isApproved(d.getIsApproved()).specialtyId(d.getSpecialty().getId()).specialtyName(d.getSpecialty().getName()).build()).toList();
 }
}
