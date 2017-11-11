package com.acme.ecommerce.controller;

import com.acme.ecommerce.domain.Product;
import com.acme.ecommerce.domain.ProductPurchase;
import com.acme.ecommerce.exception.ProductNotFoundException;
import com.acme.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Controller
@RequestMapping("/product")
public class ProductController {

	final Logger logger = LoggerFactory.getLogger(ProductController.class);

	private static final int INITIAL_PAGE = 0;
	private static final int PAGE_SIZE = 5;

	@Autowired
	ProductService productService;

	@Autowired
	HttpSession session;

	@Value("${imagePath:/images/}")
	String imagePath;

    @RequestMapping("/")
    public String index(Model model, @RequestParam(value = "page", required = false) Integer page) {
    	logger.debug("Getting Product List");
    	logger.debug("Session ID = " + session.getId());
		logger.debug("Session attributes = " + session.getAttributeNames().toString());

		// Evaluate page. If requested parameter is null or less than 0 (to
		// prevent exception), return initial size. Otherwise, return value of
		// param. decreased by 1.
		int evalPage = (page == null || page < 1) ? INITIAL_PAGE : page - 1;

    	Page<Product> products = productService.findAll(new PageRequest(evalPage, PAGE_SIZE));

		addSubtotalToModel(model);

		model.addAttribute("products", products);


        return "index";
    }


	@RequestMapping(path = "/detail/{id}", method = RequestMethod.GET)
    public String productDetail(@PathVariable long id, Model model) {
    	addSubtotalToModel(model);
    	logger.debug("Details for Product " + id);

    	Product returnProduct = productService.findById(id);

		model.addAttribute("product", returnProduct);
		ProductPurchase productPurchase = new ProductPurchase();
		productPurchase.setProduct(returnProduct);
		productPurchase.setQuantity(1);
		model.addAttribute("productPurchase", productPurchase);


        return "product_detail";
    }

    @RequestMapping(path="/{id}/image", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> productImage(@PathVariable long id) throws FileNotFoundException {
    	MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

    	logger.debug("Product Image Request for " + id);
    	logger.info("Using imagePath [" + imagePath + "]");

    	Product returnProduct = productService.findById(id);
    	String imageFilePath = null;
    	if (returnProduct != null) {
    		if (!imagePath.endsWith("/")) {
    			imagePath = imagePath + "/";
    		}
    		imageFilePath = imagePath + returnProduct.getFullImageName();
    	}
    	File imageFile = new File(imageFilePath);

    	return ResponseEntity.ok()
                .contentLength(imageFile.length())
                .contentType(MediaType.parseMediaType(mimeTypesMap.getContentType(imageFile)))
                .body(new InputStreamResource(new FileInputStream(imageFile)));
    }

    @RequestMapping(path = "/about")
    public String aboutCartShop() {
    	logger.warn("Happy Easter! Someone actually clicked on About.");
    	return("about");
    }

	private void addSubtotalToModel(Model model) {
		try {
			Object subTotal = session.getAttribute("viewTotal");
			model.addAttribute("viewTotal", subTotal);
		} catch (Exception ex) {}
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public String databaseError(Model model, Exception ex) {
		model.addAttribute("ex", ex);
		return "error";
	}
}
