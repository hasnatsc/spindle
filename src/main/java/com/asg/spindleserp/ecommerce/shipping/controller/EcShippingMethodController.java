// Path: com/asg/spindleserp/ecommerce/controller/EcShippingMethodController.java
package com.asg.spindleserp.ecommerce.shipping.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.shipping.dto.EcShippingMethodDTO;
import com.asg.spindleserp.ecommerce.shipping.service.EcShippingMethodService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecshipShow / ecshipEdit / ecshipToggle / ecshipDelete / ecshipOpenCreate */
@Controller @RequestMapping("/ecommerce/shipping-methods") @RequiredArgsConstructor
public class EcShippingMethodController {
    private final EcShippingMethodService shipService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-shipping-methods");return "ecommerce/shipping/ec-shipping-method-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){return shipService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",shipService.findById(id)));}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcShippingMethodDTO dto){Map<String,Object> res=new HashMap<>();
        try{if(dto.getId()!=null){shipService.update(dto.getId(),dto);res.put("message","Shipping method updated.");}
            else{shipService.create(dto);res.put("message","Shipping method created.");}res.put("success",true);}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/toggle/{id}") @ResponseBody
    public Map<String,Object> toggle(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{EcShippingMethodDTO d=shipService.toggleStatus(id);res.put("success",true);res.put("message","Shipping method "+(Boolean.TRUE.equals(d.getActive())?"activated":"deactivated")+".");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{shipService.delete(id);res.put("success",true);res.put("message","Shipping method deleted.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/active") @ResponseBody
    public List<Map<String,Object>> active(){Long orgId=SecurityHelper.requireOrgId();
        return shipService.findActiveByOrg(orgId).stream().map(s->{Map<String,Object> m=new LinkedHashMap<>();
            m.put("id",s.getId());m.put("code",s.getMethodCode());m.put("name",s.getMethodName());
            m.put("baseCharge",s.getBaseCharge());m.put("cod",s.getCashOnDelivery());return m;}).toList();}
}
