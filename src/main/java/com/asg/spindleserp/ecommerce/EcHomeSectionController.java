// Path: com/asg/spindleserp/ecommerce/controller/EcHomeSectionController.java
package com.asg.spindleserp.ecommerce;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: echsShow / echsEdit / echsToggle / echsDelete / echsOpenCreate */
@Controller @RequestMapping("/ecommerce/home-sections") @RequiredArgsConstructor
public class EcHomeSectionController {
    private final EcHomeSectionService sectionService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-home-sections");return "ecommerce/ec-home-section-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){return sectionService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",sectionService.findById(id)));}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcHomeSectionDTO dto){Map<String,Object> res=new HashMap<>();
        try{if(dto.getId()!=null){sectionService.update(dto.getId(),dto);res.put("message","Home section updated.");}
            else{sectionService.create(dto);res.put("message","Home section created.");}res.put("success",true);}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/toggle/{id}") @ResponseBody
    public Map<String,Object> toggle(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{EcHomeSectionDTO d=sectionService.toggleStatus(id);res.put("success",true);res.put("message","Section "+(Boolean.TRUE.equals(d.getActive())?"activated":"deactivated")+".");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{sectionService.delete(id);res.put("success",true);res.put("message","Section deleted.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/section-types") @ResponseBody
    public List<String> sectionTypes(){
        return Arrays.stream(com.asg.spindleserp.ecommerce.cms.EcHomeSection.SectionType.values()).map(Enum::name).toList();}
}
