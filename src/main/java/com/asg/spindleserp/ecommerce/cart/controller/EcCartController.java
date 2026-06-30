// Path: com/asg/spindleserp/ecommerce/controller/EcCartController.java
package com.asg.spindleserp.ecommerce.cart.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.cart.service.EcCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: eccartShow / eccartAbandon / eccartDelete */
@Controller @RequestMapping("/ecommerce/carts") @RequiredArgsConstructor
public class EcCartController {
    private final EcCartService cartService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-carts");return "ecommerce/ec-cart-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){
        return cartService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",cartService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/abandon/{id}") @ResponseBody
    public Map<String,Object> abandon(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{cartService.markAbandoned(id);res.put("success",true);res.put("message","Cart marked as abandoned.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{cartService.delete(id);res.put("success",true);res.put("message","Cart deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
}
