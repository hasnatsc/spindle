// Path: com/asg/spindleserp/ecommerce/controller/EcReturnController.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcReturnDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcReturn;
import com.asg.spindleserp.ecommerce.eCommerceReturn.service.EcReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** JS fns: ecretShow / ecretStatus / ecretDelete / ecretOpenCreate */
@Controller @RequestMapping("/ecommerce/returns") @RequiredArgsConstructor
public class EcReturnController {
    private final EcReturnService returnService;

    @GetMapping
    public String index(Model m) { m.addAttribute("activePage","ec-returns"); return "ecommerce/eCommerceReturn/ec-return-index"; }

    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="10") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return returnService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id) {
        Map<String,Object> res = new HashMap<>();
        try { res.put("success",true); res.put("obj", Map.of("defaultData", returnService.findById(id))); }
        catch (Exception e) { res.put("success",false); res.put("message",e.getMessage()); }
        return res;
    }

    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody @Valid EcReturnDTO dto) {
        Map<String,Object> res = new HashMap<>();
        try { returnService.create(dto); res.put("success",true); res.put("message","Return created."); }
        catch (Exception e) { res.put("success",false); res.put("message",e.getMessage()); }
        return res;
    }

    @PostMapping("/status/{id}") @ResponseBody
    public Map<String,Object> updateStatus(@PathVariable Long id, @RequestBody Map<String,String> body) {
        Map<String,Object> res = new HashMap<>();
        try {
            returnService.updateStatus(id, body.getOrDefault("status",""), body.get("remarks"));
            res.put("success",true); res.put("message","Return status updated.");
        } catch (Exception e) { res.put("success",false); res.put("message",e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id) {
        Map<String,Object> res = new HashMap<>();
        try { returnService.delete(id); res.put("success",true); res.put("message","Return deleted."); }
        catch (Exception e) { res.put("success",false); res.put("message",e.getMessage()); }
        return res;
    }

    @GetMapping("/statuses") @ResponseBody
    public List<String> statuses() {
        return Arrays.stream(EcReturn.ReturnStatus.values()).map(Enum::name).toList();
    }
}
