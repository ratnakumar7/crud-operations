package com.example.crudoperation.repository;

import com.example.crudoperation.entity.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepo extends JpaRepository<BankDetails,Long> {
}
