// Path: com/asg/spindleserp/ecommerce/controller/EcReviewController.java
package com.asg.spindleserp.ecommerce.review.controller;
import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.ecommerce.review.service.EcReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** JS fns: ecrevShow / ecrevApprove / ecrevReject / ecrevHide / ecrevDelete */
@Controller @RequestMapping("/ecommerce/reviews") @RequiredArgsConstructor
public class EcReviewController {
    private final EcReviewService reviewService;
    @GetMapping public String index(Model m){m.addAttribute("activePage","ec-reviews");return "ecommerce/review/ec-review-index";}
    @GetMapping("/list") @ResponseBody
    public DataTableResponse list(@RequestParam(defaultValue="1") int draw,@RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="10") int length,@RequestParam(value="search[value]",defaultValue="") String search){return reviewService.datatableList(draw,start,length,search);}
    @GetMapping("/show/{id}") @ResponseBody
    public Map<String,Object> show(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{res.put("success",true);res.put("obj",Map.of("defaultData",reviewService.findById(id)));}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/approve/{id}") @ResponseBody
    public Map<String,Object> approve(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{reviewService.approve(id);res.put("success",true);res.put("message","Review approved.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/reject/{id}") @ResponseBody
    public Map<String,Object> reject(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{reviewService.reject(id);res.put("success",true);res.put("message","Review rejected.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @PostMapping("/hide/{id}") @ResponseBody
    public Map<String,Object> hide(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{reviewService.hide(id);res.put("success",true);res.put("message","Review hidden.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
    @DeleteMapping("/delete/{id}") @ResponseBody
    public Map<String,Object> delete(@PathVariable Long id){Map<String,Object> res=new HashMap<>();
        try{reviewService.delete(id);res.put("success",true);res.put("message","Review deleted.");}catch(Exception e){res.put("success",false);res.put("message",e.getMessage());}return res;}
}
