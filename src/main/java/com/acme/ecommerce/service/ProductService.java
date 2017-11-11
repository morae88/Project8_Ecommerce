package com.acme.ecommerce.service;

import com.acme.ecommerce.domain.Product;
import com.acme.ecommerce.exception.InsufficientQuantityException;
import com.acme.ecommerce.exception.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

	public Iterable<Product> findAll();

	public Page<Product> findAll(Pageable pageable);

	public Product findById(Long id) throws ProductNotFoundException;

	void checkQuantity(int requested, int actual) throws InsufficientQuantityException;
}
