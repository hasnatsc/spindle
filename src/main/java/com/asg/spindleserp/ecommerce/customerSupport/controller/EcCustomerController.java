// Path: com/asg/spindleserp/ecommerce/controller/EcCustomerController.java
package com.asg.spindleserp.ecommerce.customerSupport.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.customerSupport.dto.EcCustomerDTO;
import com.asg.spindleserp.ecommerce.customerSupport.service.EcCustomerService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** JS fns: eccustShow / eccustEdit / eccustToggle / eccustDelete / eccustOpenCreate */
@Controller @RequestMapping("/ecommerce/customers") @RequiredArgsConstructor
public class EcCustomerController {
    private final EcCustomerService customerService;

    @GetMapping public String index(Model model) { model.addAttribute("activePage","ec-customers"); return "ecommerce/customerSupport/ec-customer-index"; }

    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw, @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length, @RequestParam(value="search[value]",defaultValue="") String search) {
        return customerService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id) {
        Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",customerService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}
        return res;
    }

    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcCustomerDTO dto) {
        Map<String,Object> res=new HashMap<>();
        try {
            if(dto.getId()!=null){customerService.update(dto.getId(),dto);res.put("message","Customer updated.");}
            else{customerService.create(dto);res.put("message","Customer created.");}
            res.put("success",true);
        } catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}
        return res;
    }

    @PostMapping("/toggle/{id}") @ResponseBody
    public Map<String,Object> toggle(@PathVariable Long id) {
        Map<String,Object> res=new HashMap<>();
        try{EcCustomerDTO d=customerService.toggleStatus(id);res.put("success",true);res.put("message","Customer "+(Boolean.TRUE.equals(d.getActive())?"activated":"blocked")+".");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}
        return res;
    }

    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id) {
        Map<String,Object> res=new HashMap<>();
        try{customerService.delete(id);res.put("success",true);res.put("message","Customer deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}
        return res;
    }

    @GetMapping("/active") @ResponseBody
    public List<Map<String,Object>> active() {
        Long orgId=SecurityHelper.requireOrgId();
        return customerService.findActiveByOrg(orgId).stream().map(c->{
            Map<String,Object> m=new LinkedHashMap<>();m.put("id",c.getId());m.put("name",c.getFullName());m.put("phone",c.getPhone());return m;}).toList();
    }
}
