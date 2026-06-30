// Path: com/asg/spindleserp/ecommerce/controller/EcOrderController.java
package com.asg.spindleserp.ecommerce.order.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import com.asg.spindleserp.ecommerce.order.service.EcOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecordShow / ecordStatus / ecordDelete */
@Controller @RequestMapping("/ecommerce/orders") @RequiredArgsConstructor
public class EcOrderController {
    private final EcOrderService orderService;
    @GetMapping public String index(Model model) { model.addAttribute("activePage","ec-orders"); return "ecommerce/order/ec-order-index"; }
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search) {
        return orderService.datatableList(draw,start,length,search);
    }
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){
        Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",orderService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;
    }
    @PostMapping("/status/{id}") @ResponseBody
    public Map<String,Object> updateStatus(@PathVariable Long id,@RequestBody Map<String,String> body){
        Map<String,Object> res=new HashMap<>();
        try{orderService.updateStatus(id,body.getOrDefault("status",""),body.get("adminNote"));
            res.put("success",true);res.put("message","Order status updated.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;
    }
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){
        Map<String,Object> res=new HashMap<>();
        try{orderService.delete(id);res.put("success",true);res.put("message","Order deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;
    }
    /** OrderStatus enum values for dropdown */
    @GetMapping("/statuses") @ResponseBody
    public List<String> statuses(){
        return Arrays.stream(EcOrder.OrderStatus.values()).map(Enum::name).toList();
    }
}
