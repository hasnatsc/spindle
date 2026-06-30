// Path: com/asg/spindleserp/ecommerce/controller/EcRefundController.java
package com.asg.spindleserp.ecommerce.eCommerceReturn.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.eCommerceReturn.dto.EcRefundDTO;
import com.asg.spindleserp.ecommerce.eCommerceReturn.entity.EcRefund;
import com.asg.spindleserp.ecommerce.eCommerceReturn.service.EcRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecrefShow / ecrefDelete / ecrefOpenCreate */
@Controller @RequestMapping("/ecommerce/refunds") @RequiredArgsConstructor
public class EcRefundController {
    private final EcRefundService refundService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-refunds");return "ecommerce/eCommerceReturn/ec-refund-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){
        return refundService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",refundService.findById(id)));}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/save") @ResponseBody
    public Map<String,Object> save(@RequestBody EcRefundDTO dto){Map<String,Object> res=new HashMap<>();
        try{refundService.create(dto);res.put("success",true);res.put("message","Refund created.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{refundService.delete(id);res.put("success",true);res.put("message","Refund deleted.");}
        catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @GetMapping("/methods") @ResponseBody
    public List<String> methods(){return Arrays.stream(EcRefund.RefundMethod.values()).map(Enum::name).toList();}
}
