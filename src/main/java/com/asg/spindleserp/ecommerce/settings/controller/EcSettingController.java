// Path: com/asg/spindleserp/ecommerce/controller/EcSettingController.java
package com.asg.spindleserp.ecommerce.settings.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.settings.dto.EcSettingDTO;
import com.asg.spindleserp.ecommerce.settings.service.EcSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecstShow / ecstEdit / ecstDelete / ecstOpenCreate */
@Controller @RequestMapping("/ecommerce/settings") @RequiredArgsConstructor
public class EcSettingController {
    private final EcSettingService settingService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-settings");return "ecommerce/settings/ec-setting-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){
        return settingService.datatableList(draw,start,length,search);}
    @GetMapping("/grouped") @ResponseBody
    public Map<String,Object> grouped(){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",settingService.findAllGrouped());}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",settingService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcSettingDTO dto){Map<String,Object> res=new HashMap<>();
        try{settingService.createOrUpdate(dto);res.put("success",true);res.put("message","Setting saved.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/bulk-save") @ResponseBody
    public Map<String,Object> bulkSave(@RequestBody List<EcSettingDTO> dtos){Map<String,Object> res=new HashMap<>();
        try{settingService.bulkSave(dtos);res.put("success",true);res.put("message",dtos.size()+" settings saved.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{settingService.delete(id);res.put("success",true);res.put("message","Setting deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
}
