package com.janvoice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Set;

public class RecyclerRequest {
    @NotBlank private String businessName;
    private String contactName;
    @NotBlank @Pattern(regexp = "^[+]?[0-9 ()-]{10,20}$", message = "phone must be a valid phone number") private String phone;
    private String address;
    @NotBlank private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @NotEmpty private Set<Long> acceptedMaterialIds;
    private boolean pickupAvailable;
    private BigDecimal pickupRadius;
    private String serviceArea;
    private String registrationNumber;

    public String getBusinessName(){return businessName;} public void setBusinessName(String v){businessName=v;} public String getContactName(){return contactName;} public void setContactName(String v){contactName=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;} public String getCity(){return city;} public void setCity(String v){city=v;} public BigDecimal getLatitude(){return latitude;} public void setLatitude(BigDecimal v){latitude=v;} public BigDecimal getLongitude(){return longitude;} public void setLongitude(BigDecimal v){longitude=v;} public Set<Long> getAcceptedMaterialIds(){return acceptedMaterialIds;} public void setAcceptedMaterialIds(Set<Long> v){acceptedMaterialIds=v;} public boolean isPickupAvailable(){return pickupAvailable;} public void setPickupAvailable(boolean v){pickupAvailable=v;} public BigDecimal getPickupRadius(){return pickupRadius;} public void setPickupRadius(BigDecimal v){pickupRadius=v;} public String getServiceArea(){return serviceArea;} public void setServiceArea(String v){serviceArea=v;} public String getRegistrationNumber(){return registrationNumber;} public void setRegistrationNumber(String v){registrationNumber=v;}
}
