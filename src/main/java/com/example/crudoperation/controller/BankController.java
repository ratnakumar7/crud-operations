package com.example.crudoperation.controller;

import com.example.crudoperation.entity.BankDetails;
import com.example.crudoperation.repository.BankRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class BankController {
    // create a method which will perform the database operations
    @Autowired
    private BankRepo repo;

    @PostMapping(value = "/insert")
    public ResponseEntity<BankDetails> insertDetails(@RequestBody BankDetails bankDetails) {
        repo.save(bankDetails);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // to retrieve all the bank details
    @GetMapping("/get")
    public ResponseEntity<List<BankDetails>> getBankDetails() {
        return new ResponseEntity<>(repo.findAll(), HttpStatus.OK);
    }

    // to retrieve the data by using id
    @GetMapping("/get1/{accountNumber}")
    public ResponseEntity<BankDetails> getOne(@PathVariable long accountNumber) {
        Optional<BankDetails> bankDetails = repo.findById(accountNumber);
        if (bankDetails.isPresent()) {
            return new ResponseEntity<>(bankDetails.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // to perform update operation
    }

    @PutMapping("/updateData/{accountNumber}")
    public ResponseEntity<BankDetails> updateData(@PathVariable long accountNumber, @RequestBody BankDetails bankDetails) {
        Optional<BankDetails> bankDetails1 = repo.findById(accountNumber);
        if (bankDetails1.isPresent()) {
            bankDetails1.get().setAccountHolderName(bankDetails.getAccountHolderName());
            bankDetails1.get().setAadharNumber(bankDetails.getAadharNumber());
            bankDetails1.get().setMobileNumber(bankDetails.getMobileNumber());
            return new ResponseEntity<>(repo.save(bankDetails1.get()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // to perform the delete operation
    @DeleteMapping("/deleteData/{accountNumber}")
    public ResponseEntity<Void> deleteData(@PathVariable long accountNumber) {
        Optional<BankDetails> bankDetails = repo.findById(accountNumber);
        if (bankDetails.isPresent()) {
            repo.deleteById(accountNumber);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
