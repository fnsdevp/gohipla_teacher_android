package com.fnspl.hiplaedu_teacher.model;

/**
 * Created by FNSPL on 9/15/2017.
 */

public class StudentOutOfClassData {

    private int student_id;
    private String name;
    private String phone;
    private String address;
    private String email;
    private String photo;
    private String present;
    private String remark;
    private String student_in_time;
    private String student_out_time;

    public String getStudent_in_time() {
        return student_in_time;
    }

    public void setStudent_in_time(String student_in_time) {
        this.student_in_time = student_in_time;
    }

    public String getStudent_out_time() {
        return student_out_time;
    }

    public void setStudent_out_time(String student_out_time) {
        this.student_out_time = student_out_time;
    }

    public int getStudent_id() {
        return student_id;
    }

    public void setStudent_id(int student_id) {
        this.student_id = student_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPresent() {
        return present;
    }

    public void setPresent(String present) {
        this.present = present;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
