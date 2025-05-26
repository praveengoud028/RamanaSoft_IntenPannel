package com.ramanasoft.www.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ramanasoft.www.model.InternModel;

public interface InternRepository extends JpaRepository<InternModel, Long> 
{
	InternModel findByMobile(String mobile);
}
