package com.acme.ecommerce.controller;

import com.acme.ecommerce.Application;
import com.acme.ecommerce.FlashMessage;
import com.acme.ecommerce.domain.Product;
import com.acme.ecommerce.domain.ProductPurchase;
import com.acme.ecommerce.domain.Purchase;
import com.acme.ecommerce.domain.ShoppingCart;
import com.acme.ecommerce.exception.InsufficientQuantityException;
import com.acme.ecommerce.service.ProductService;
import com.acme.ecommerce.service.PurchaseService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class CartControllerTest {

	final String BASE_URL = "http://localhost:8080/";

	@Mock
	private MockHttpSession session;

	@Mock
	private ProductService productService;
	@Mock
	private PurchaseService purchaseService;
	@Mock
	private ShoppingCart sCart;
	@InjectMocks
	private CartController cartController;

	private MockMvc mockMvc;

	static {
		System.setProperty("properties.home", "properties");
	}

	@Before
	public void setup() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setPrefix("/WEB-INF/");
		viewResolver.setSuffix(".html");

		MockitoAnnotations.initMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(cartController).setViewResolvers(viewResolver).build();
	}

	@Test
	public void viewCartTest() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		Purchase purchase = purchaseBuilder(product);

		when(sCart.getPurchase()).thenReturn(purchase);
		mockMvc.perform(MockMvcRequestBuilders.get("/cart")).andDo(print())
				.andExpect(status().isOk())
				.andExpect(view().name("cart"));
	}

	@Test
	public void viewCartNoPurchasesTest() throws Exception {

		when(sCart.getPurchase()).thenReturn(null);
		mockMvc.perform(MockMvcRequestBuilders.get("/cart")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void addToCartTest() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/add").param("quantity", "1").param("productId", "1"))
				.andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/product/"));
	}

	@Test
	public void addUnknownToCartTest() throws Exception {
		when(productService.findById(1L)).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/add").param("quantity", "1").param("productId", "1"))
				.andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void cartQuantityExceedsStockTest() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		doThrow(new InsufficientQuantityException()).when(productService).checkQuantity(anyInt(), anyInt());

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/add")
				.param("quantity", "4")
				.param("productId", "1")
				.header("referer", "/product/"))
				.andDo(print())
				.andExpect(redirectedUrl("/product/"))
				.andExpect(model().attribute("ex", Matchers.instanceOf(InsufficientQuantityException.class)))
				.andExpect(flash().attribute("flash",Matchers.hasProperty("status",Matchers.equalTo(FlashMessage.Status.FAILURE))));
	}

	@Test
	public void updateCartTest() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		Purchase purchase = purchaseBuilder(product);

		when(sCart.getPurchase()).thenReturn(purchase);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/update").param("newQuantity", "2").param("productId", "1"))
				.andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"))
				.andExpect(flash().attribute("flash",Matchers.hasProperty("status",Matchers.equalTo(FlashMessage.Status.SUCCESS))));
	}

	@Test
	public void updateUnknownCartTest() throws Exception {
		when(productService.findById(1L)).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/update").param("newQuantity", "2").param("productId", "1"))
				.andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void updateInvalidCartTest() throws Exception {

		when(sCart.getPurchase()).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/update").param("newQuantity", "2").param("productId", "1"))
				.andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void removeFromCartTest() throws Exception {
		Product product = productBuilder();

		Product product2 = productBuilder();
		product2.setId(2L);

		when(productService.findById(1L)).thenReturn(product);

		ProductPurchase pp = new ProductPurchase();
		pp.setProductPurchaseId(1L);
		pp.setQuantity(1);
		pp.setProduct(product);

		ProductPurchase pp2 = new ProductPurchase();
		pp2.setProductPurchaseId(2L);
		pp2.setQuantity(2);
		pp2.setProduct(product2);

		List<ProductPurchase> ppList = new ArrayList<ProductPurchase>();
		ppList.add(pp);
		ppList.add(pp2);

		Purchase purchase = new Purchase();
		purchase.setId(1L);
		purchase.setProductPurchases(ppList);

		when(sCart.getPurchase()).thenReturn(purchase);

		when(purchaseService.save(purchase)).thenReturn(purchase);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/remove").param("productId", "1")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/cart"))
				.andExpect(flash().attribute("flash",Matchers.hasProperty("status",Matchers.equalTo(FlashMessage.Status.SUCCESS))));
	}

	@Test
	public void removeUnknownCartTest() throws Exception {
		when(productService.findById(1L)).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/remove").param("productId", "1")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void removeInvalidCartTest() throws Exception {

		when(sCart.getPurchase()).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/remove").param("productId", "1")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void removeLastFromCartTest() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		Purchase purchase = purchaseBuilder(product);

		when(sCart.getPurchase()).thenReturn(purchase);

		when(purchaseService.save(purchase)).thenReturn(purchase);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/remove").param("productId", "1")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/product/"))
				.andExpect(flash().attribute("flash",Matchers.hasProperty("status",Matchers.equalTo(FlashMessage.Status.SUCCESS))));
	}

	@Test
	public void emptyCartTest() throws Exception {
		Product product = productBuilder();

		Product product2 = productBuilder();
		product2.setId(2L);

		when(productService.findById(1L)).thenReturn(product);

		ProductPurchase pp = new ProductPurchase();
		pp.setProductPurchaseId(1L);
		pp.setQuantity(1);
		pp.setProduct(product);

		ProductPurchase pp2 = new ProductPurchase();
		pp2.setProductPurchaseId(2L);
		pp2.setQuantity(2);
		pp2.setProduct(product2);

		List<ProductPurchase> ppList = new ArrayList<ProductPurchase>();
		ppList.add(pp);
		ppList.add(pp2);

		Purchase purchase = new Purchase();
		purchase.setId(1L);
		purchase.setProductPurchases(ppList);

		when(sCart.getPurchase()).thenReturn(purchase);

		when(purchaseService.save(purchase)).thenReturn(purchase);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/empty")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/product/"))
				.andExpect(flash().attribute("flash",Matchers.hasProperty("status",Matchers.equalTo(FlashMessage.Status.SUCCESS))));
	}

	@Test
	public void emptyInvalidCartTest() throws Exception {

		when(sCart.getPurchase()).thenReturn(null);

		mockMvc.perform(MockMvcRequestBuilders.post("/cart/empty")).andDo(print())
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/error"));
	}

	@Test
	public void cart_ShouldShowValue() throws Exception {
		Product product = productBuilder();

		when(productService.findById(1L)).thenReturn(product);

		Purchase purchase = purchaseBuilder(product);

		when(sCart.getPurchase()).thenReturn(purchase);
		mockMvc.perform(MockMvcRequestBuilders.get("/cart")).andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("viewTotal", product.getPrice()));

	}

	private Product productBuilder() {
		Product product = new Product();
		product.setId(1L);
		product.setDesc("TestDesc");
		product.setName("TestName");
		product.setPrice(new BigDecimal(1.99));
		product.setQuantity(3);
		product.setFullImageName("imagename");
		product.setThumbImageName("imagename");
		return product;
	}

	private Purchase purchaseBuilder(Product product) {
		ProductPurchase pp = new ProductPurchase();
		pp.setProductPurchaseId(1L);
		pp.setQuantity(1);
		pp.setProduct(product);
		List<ProductPurchase> ppList = new ArrayList<ProductPurchase>();
		ppList.add(pp);

		Purchase purchase = new Purchase();
		purchase.setId(1L);
		purchase.setProductPurchases(ppList);
		return purchase;
	}
}
