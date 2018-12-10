package com.zixu.paysapi.jpa.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="com_zixu_admin_config")
public class AdminConfig implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column(name="id")
	private String id;
	
	@Column(name="type")
	private String type;
	
	@Column(name="procedures")
	private int procedures;
	
	@Column(name="overdueTime")
	private int overdueTime;
	
	@Column(name="immediately")
	private String immediately;

	public int getOverdueTime() {
		return overdueTime;
	}

	public void setOverdueTime(int overdueTime) {
		this.overdueTime = overdueTime;
	}

	public String getImmediately() {
		return immediately;
	}

	public void setImmediately(String immediately) {
		this.immediately = immediately;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getProcedures() {
		return procedures;
	}

	public void setProcedures(int procedures) {
		this.procedures = procedures;
	}
	
}
