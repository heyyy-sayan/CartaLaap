package com.cartalaap.community;
import jakarta.validation.constraints.*;
public record CreateCommunityRequest(@NotBlank @Size(min=2,max=51) String name,@Size(max=300) String description){}
