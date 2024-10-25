# Overview

### Email Verification

This API enables your application to verify that an email is active.

### What and how?
The `/send-code` endpoint is used to send a unique verification code to the provided email. This code is then provided to the calling service by the citizen.
The `/verify-code` endpoint is used to verify that the email & verification code provided match the ones on record.

### How to use the results


### Request\response details

All requests must include a uniquely identifiable `user-agent` header. Please contact us for assistance when first connecting.  

* POST /send-code
    * [Request](send-code-request-sample.json) ([schema](send-code-request.json))
    * [Response](verify-code-response-sample.json) ([schema](send-code-response.json))
* 
* POST /verify-code
    * [Request](verify-code-request-sample.json) ([schema](verify-code-request.json))
    * [Response](verify-code-response-sample.json) ([schema](verify-code-response.json))
