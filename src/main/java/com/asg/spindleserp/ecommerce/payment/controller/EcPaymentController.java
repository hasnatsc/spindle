// Path: com/asg/spindleserp/ecommerce/controller/EcPaymentController.java
package com.asg.spindleserp.ecommerce.payment.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.payment.entity.EcPayment;
import com.asg.spindleserp.ecommerce.payment.service.EcPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecpayShow / ecpayStatus / ecpayDelete */
@Controller @RequestMapping("/ecommerce/payments") @RequiredArgsConstructor
public class EcPaymentController {
    private final EcPaymentService paymentService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-payments");return "ecommerce/payment/ec-payment-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){
        return paymentService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",paymentService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/status/{id}") @ResponseBody
    public Map<String,Object> updateStatus(@PathVariable Long id,@RequestBody Map<String,String> body){Map<String,Object> res=new HashMap<>();
        try{paymentService.updateStatus(id,body.getOrDefault("status",""),body.get("remarks"));res.put("success",true);res.put("message","Payment status updated.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{paymentService.delete(id);res.put("success",true);res.put("message","Payment deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/statuses") @ResponseBody
    public List<String> statuses(){return Arrays.stream(EcPayment.PaymentStatus.values()).map(Enum::name).toList();}
    @GetMapping("/methods") @ResponseBody
    public List<String> methods(){return Arrays.stream(EcPayment.PaymentMethod.values()).map(Enum::name).toList();}
}
