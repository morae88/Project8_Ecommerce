package com.acme.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such product id")
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) { super("Product " + id +" not found."); }
}
