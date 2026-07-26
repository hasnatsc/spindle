package com.asg.spindleserp.common.controller;

import com.asg.spindleserp.common.enums.ApprovalStatus;
import com.asg.spindleserp.common.enums.DocumentType;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.travel.entity.TrvBookingReceipt;
import com.asg.spindleserp.travel.entity.TrvBookingService;
import com.asg.spindleserp.travel.entity.TrvPassenger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised enum endpoint — returns all enums as {value, display} lists.
 *
 * Any page can load dropdown options in a single call:
 * <pre>{@code
 *   secureFetch('/enums/all').then(data => {
 *       asgLoadDropdown({elementId: 'itemType',       data: data.itemType});
 *       asgLoadDropdown({elementId: 'approvalStatus',  data: data.approvalStatus});
 *       asgLoadDropdown({elementId: 'movementType',    data: data.movementType});
 *       asgLoadDropdown({elementId: 'documentType',    data: data.documentType});
 *       asgLoadDropdown({elementId: 'voucherType',     data: data.voucherType});
 *       asgLoadDropdown({elementId: 'serviceType',     data: data.serviceType});
 *       asgLoadDropdown({elementId: 'passengerType',   data: data.passengerType});
 *       asgLoadDropdown({elementId: 'paymentMode',     data: data.paymentMode});
 *   });
 * }</pre>
 */
@RestController
public class EnumController {

    @GetMapping("/enums/all")
    @ResponseBody
    public Map<String, Object> getAllEnums() {
        Map<String, Object> result = new HashMap<>();
        result.put("itemType",       CommonUtils.mapEnumWithDisplay(ItemType.values()));
        result.put("approvalStatus", CommonUtils.mapEnumWithDisplay(ApprovalStatus.values()));
        result.put("movementType",   CommonUtils.mapEnumWithDisplay(MovementType.values()));
        result.put("documentType",   CommonUtils.mapEnumWithDisplay(DocumentType.values()));
        result.put("voucherType",    CommonUtils.mapEnumWithDisplay(VoucherType.values()));
        result.put("serviceType",    CommonUtils.mapEnumWithDisplay(TrvBookingService.ServiceType.values()));
        result.put("passengerType",  CommonUtils.mapEnumWithDisplay(TrvPassenger.PassengerType.values()));
        result.put("paymentMode",    mapPaymentModes());
        return result;
    }

    /** Payment mode with GL account type (CASH / BANK) for sub-account filtering. */
    private Object mapPaymentModes() {
        return Arrays.stream(TrvBookingReceipt.PaymentMode.values())
                .map(m -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("value",       m.name());
                    entry.put("display",     CommonUtils.formatEnumLabel(m.name()));
                    entry.put("accountType", m.getAccountType());
                    return entry;
                })
                .toList();
    }
}
