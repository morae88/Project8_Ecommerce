package com.acme.ecommerce.service;

import com.acme.ecommerce.domain.Product;
import com.acme.ecommerce.exception.InsufficientQuantityException;
import com.acme.ecommerce.exception.ProductNotFoundException;
import com.acme.ecommerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository repository;

	@Autowired
    public ProductServiceImpl(ProductRepository repository) {
        this.repository = repository;
    }

	@Transactional
	@Override
	public Iterable<Product> findAll() {
		return repository.findAll();
	}

	@Override
	public Page<Product> findAll(Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Override
	public Product findById(Long id) {
		Product result = repository.findOne(id);

		if (result == null) {
			throw new ProductNotFoundException(id);
		}

		return result;
	}

	@Override
	public void checkQuantity(int requested, int actual) throws InsufficientQuantityException {
		if (requested > actual) {
			throw new InsufficientQuantityException();
		}
	}

}
