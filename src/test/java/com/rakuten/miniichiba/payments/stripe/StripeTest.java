package com.rakuten.miniichiba.payments.stripe;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class StripeTest {
    private final static String PUBLIC_KEY = "pk_test_6wq9WTgcjzKfd62oo5pjInku00WUp5Fdvk";
    private final static String SECRET_KEY = "sk_test_t3xBJqQYkL8jzWLKsdz4X4Sq00NNBbVmbd";
    /**
     * Rigorous Test :-)
     */
    @Test
    public void simpleTransaction() {
        // Set your secret key: remember to change this to your live secret key in production
        // See your keys here: https://dashboard.stripe.com/account/apikeys
        Stripe.apiKey = "sk_test_t3xBJqQYkL8jzWLKsdz4X4Sq00NNBbVmbd";

        Map<String, Object> params = new HashMap<String, Object>();

        ArrayList<String> paymentMethodTypes = new ArrayList<>();
        paymentMethodTypes.add("card");
        params.put("payment_method_types", paymentMethodTypes);

        ArrayList<HashMap<String, Object>> lineItems = new ArrayList<>();
        HashMap<String, Object> lineItem = new HashMap<String, Object>();
        lineItem.put("name", "T-shirt");
        lineItem.put("description", "Comfortable cotton t-shirt");
        lineItem.put("amount", 500);
        lineItem.put("currency", "usd");
        lineItem.put("quantity", 1);
        lineItems.add(lineItem);
        params.put("line_items", lineItems);

        params.put("success_url", "https://example.com/success");
        params.put("cancel_url", "https://example.com/cancel");
        try {
            Session session = Session.create(params);
            String id = session.getId();
        } catch (StripeException e) {
            e.printStackTrace();
        }
    }
}
