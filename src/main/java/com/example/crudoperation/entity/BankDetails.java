package com.example.crudoperation.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

// to represent the class as an entity use @Entity annotation
@Entity
public class BankDetails {
    @Id // to represent primary key
    // if we want to auto generate the id or serial numbers make use @GenerateValue(strategy = GenerationType.AUTO)
    private long accountNumber;
    private String accountHolderName;
    private long mobileNumber;
    private long aadharNumber;

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public long getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(long mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public long getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(long aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

    public BankDetails(long accountNumber, String accountHolderName, long mobileNumber, long aadharNumber) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.mobileNumber = mobileNumber;
        this.aadharNumber = aadharNumber;
    }

    public BankDetails() {
    }

    @Override
    public String toString() {
        return "BankDetails{" +
                "accountNumber=" + accountNumber +

                ", accountHolderName='" + accountHolderName + '\'' +
                ", mobileNumber=" + mobileNumber +
                ", aadharNumber=" + aadharNumber +
                '}';
    }
}
