// Path: com/asg/spindleserp/ecommerce/controller/EcCouponController.java
package com.asg.spindleserp.ecommerce.campaign.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.campaign.dto.EcCouponDTO;
import com.asg.spindleserp.ecommerce.campaign.service.EcCouponService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecoupShow / ecoupEdit / ecoupToggle / ecoupDelete / ecoupOpenCreate */
@Controller @RequestMapping("/ecommerce/coupons") @RequiredArgsConstructor
public class EcCouponController {
    private final EcCouponService couponService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-coupons");return "ecommerce/campaign/ec-coupon-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){
        return couponService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",couponService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcCouponDTO dto){Map<String,Object> res=new HashMap<>();
        try{if(dto.getId()!=null){couponService.update(dto.getId(),dto);res.put("message","Coupon updated.");}
            else{couponService.create(dto);res.put("message","Coupon created.");}res.put("success",true);}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/toggle/{id}") @ResponseBody
    public Map<String,Object> toggle(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{
            EcCouponDTO d=couponService.toggleStatus(id);res.put("success",true);res.put("message","Coupon "+(Boolean.TRUE.equals(d.getActive())?"activated":"deactivated")+".");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{couponService.delete(id);res.put("success",true);res.put("message","Coupon deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/active") @ResponseBody
    public List<Map<String,Object>> active(){Long orgId=SecurityHelper.requireOrgId();
        return couponService.findActiveByOrg(orgId).stream().map(c->{Map<String,Object> m=new LinkedHashMap<>();
            m.put("id",c.getId());m.put("code",c.getCouponCode());m.put("name",c.getCouponName());
            m.put("discountType",c.getDiscountType());m.put("discountValue",c.getDiscountValue());return m;}).toList();}
}
