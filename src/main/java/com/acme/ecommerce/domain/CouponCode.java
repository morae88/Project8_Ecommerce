package com.acme.ecommerce.domain;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Component
@Scope("session")
public class CouponCode {


	@Pattern(message="If using coupon code, code must be between 5-10 characters long ", regexp="^(|[a-zA-Z0-9]{5,10})$")
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
