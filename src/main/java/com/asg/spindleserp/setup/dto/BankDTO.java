package com.asg.spindleserp.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankDTO {

    private Long id;

    @NotBlank(message = "Bank code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String bankCode;

    @NotBlank(message = "Bank name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String bankName;

    @Size(max = 200) private String bankNameLocal;
    @Size(max = 50)  private String shortName;

    @Builder.Default
    private String bankType = "COMMERCIAL";
    @Size(max = 30) private String bankCategory;

    @Size(max = 11) private String swiftCode;
    @Size(max = 20) private String centralBankCode;
    @Size(max = 9)  private String routingNumberPrefix;

    // Head office
    @Size(max = 500) private String headOfficeAddress;
    @Size(max = 100) private String headOfficeCity;
    @Size(max = 100) private String headOfficeCountry;
    @Size(max = 50)  private String headOfficePhone;
    @Size(max = 100) private String headOfficeEmail;
    @Size(max = 200) private String website;

    // Correspondent bank
    @Size(max = 200) private String correspondentBankName;
    @Size(max = 11)  private String correspondentSwiftCode;
    @Size(max = 50)  private String correspondentAccountNumber;

    @Builder.Default
    private String rating = "UNRATED";

    // Feature flags
    @Builder.Default private Boolean supportsLc             = false;
    @Builder.Default private Boolean supportsImportLc       = false;
    @Builder.Default private Boolean supportsExportLc       = false;
    @Builder.Default private Boolean supportsBtbLc          = false;
    @Builder.Default private Boolean supportsInlandLc       = false;
    @Builder.Default private Boolean supportsOnlineBanking  = false;

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
