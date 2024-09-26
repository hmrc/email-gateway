# Overview

### Email Verification

This API enables your application to verify that an email exists.

### What and how?
The `/verify` endpoint is used to send a unique passcode to the provided email. This passcode is then returned to the calling service by the citizen.
The `/verify/passcode` endpoint is used to verify that the email & passcode provided match the ones on record.

### How to use the results


### Request\response details

All requests must include a uniquely identifiable `user-agent` header. Please contact us for assistance when first connecting.  

* POST /verify
    * [Request](verify-request-sample.json) ([schema](verify-request.json))
    * [Response](verify-response-sample.json) ([schema](risk-response.json))
* 
* POST /verify/passcode
    * [Request](verify-request-sample.json) ([schema](verify-request.json))
    * [Response](verify-response-sample.json) ([schema](risk-response.json))
