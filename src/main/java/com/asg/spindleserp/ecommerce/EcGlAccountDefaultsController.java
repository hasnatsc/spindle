// Path: com/asg/spindleserp/ecommerce/controller/EcGlAccountDefaultsController.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** Singleton settings page — no DataTable needed */
@Controller @RequestMapping("/ecommerce/gl-defaults") @RequiredArgsConstructor
public class EcGlAccountDefaultsController {
    private final EcGlAccountDefaultsService glService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-gl-defaults");return "ecommerce/ec-gl-defaults";}
    @GetMapping("/load") @ResponseBody
    public Map<String,Object> load(){Map<String,Object> res=new HashMap<>();
        try{Long orgId=SecurityHelper.requireOrgId();res.put("success",true);res.put("obj",glService.findOrCreateByOrg(orgId));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody EcGlAccountDefaultsDTO dto){Map<String,Object> res=new HashMap<>();
        try{glService.save(dto);res.put("success",true);res.put("message","GL account defaults saved.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
}
