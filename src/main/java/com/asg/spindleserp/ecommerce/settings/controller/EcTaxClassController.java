// Path: com/asg/spindleserp/ecommerce/controller/EcTaxClassController.java
package com.asg.spindleserp.ecommerce.settings.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.settings.dto.EcTaxClassDTO;
import com.asg.spindleserp.ecommerce.settings.service.EcTaxClassService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ectaxShow / ectaxEdit / ectaxToggle / ectaxDelete / ectaxOpenCreate */
@Controller @RequestMapping("/ecommerce/tax-classes") @RequiredArgsConstructor
public class EcTaxClassController {
    private final EcTaxClassService taxService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-tax-classes");return "ecommerce/settings/ec-tax-class-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){return taxService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",taxService.findById(id)));}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcTaxClassDTO dto){Map<String,Object> res=new HashMap<>();
        try{if(dto.getId()!=null){taxService.update(dto.getId(),dto);res.put("message","Tax class updated.");}
            else{taxService.create(dto);res.put("message","Tax class created.");}res.put("success",true);}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/toggle/{id}") @ResponseBody
    public Map<String,Object> toggle(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{EcTaxClassDTO d=taxService.toggleStatus(id);res.put("success",true);res.put("message","Tax class "+(Boolean.TRUE.equals(d.getActive())?"activated":"deactivated")+".");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{taxService.delete(id);res.put("success",true);res.put("message","Tax class deleted.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/active") @ResponseBody
    public List<Map<String,Object>> active(){Long orgId=SecurityHelper.requireOrgId();
        return taxService.findActiveByOrg(orgId).stream().map(t->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",t.getId());m.put("code",t.getClassCode());m.put("name",t.getClassName());return m;}).toList();}
}
