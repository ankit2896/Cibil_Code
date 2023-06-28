package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.constants.ApiUrl;
import com.freecharge.cibil.exceptions.FCBadRequestException;
import com.freecharge.cibil.exceptions.ValidationException;
import com.freecharge.cibil.model.enums.ErrorCodeAndMessage;
import com.freecharge.cibil.model.request.ClientPurposeOnboardingRequest;
import com.freecharge.cibil.model.request.ClientPurposeUpdationRequest;
import com.freecharge.cibil.model.response.GetAllClientForPurposeResponse;
import com.freecharge.cibil.model.response.GetAllPurposeForClientResponse;
import com.freecharge.cibil.model.response.ServiceResponse;
import com.freecharge.cibil.mysql.builder.ValidityConfigBuilder;
import com.freecharge.cibil.mysql.model.ValidityConfigModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

import static com.freecharge.cibil.constants.HeaderConstants.CLIENT_KEY;
import static com.freecharge.cibil.constants.HeaderConstants.PURPOSE_KEY;

@Slf4j
@RestController
public class PurposeRecordsController {

    private final ValidityConfigBuilder builder;

    @Autowired
    public PurposeRecordsController(@NonNull final ValidityConfigBuilder builder) {
        this.builder = builder;
    }

	@Logged
	@Timed
	@Marked
	@PostMapping(ApiUrl.ONBOARD_NEW_PURPOSE)
	public ServiceResponse<Boolean> onboardNewPurpose(
			@RequestBody @Valid final ClientPurposeOnboardingRequest request) {
		log.info("onboarding request for client {} and purpose {} with validity {}", request.getClientId(),
				request.getPurpose(), request.getValidity());
		final ValidityConfigModel model = builder.getConfig(request.getClientId(), request.getPurpose());
		if (ObjectUtils.isNotEmpty(model)) {
			log.error("Client {} Purpose {} already on-boarded", request.getClientId(), request.getPurpose());
			throw new FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_ALREADY_ONBOARD);
		}else if(request.getValidity() < 0) {
			log.error("Validity Can not be -ve", request.getValidity());
			throw new ValidationException(ErrorCodeAndMessage.VALIDITY_CAN_NOT_BE_NULL);
		}
		builder.insert(request.getClientId(), request.getPurpose(), request.getValidity());
		return new ServiceResponse<>(true, true);
	}

	@Logged
	@Timed
	@Marked
	@PutMapping(ApiUrl.EDIT_PURPOSE_FOR_CLIENT)
	public ServiceResponse<Integer> editPurposeForClient(
			@RequestBody @Valid final ClientPurposeUpdationRequest request) {
		log.info("EDIT request for client {} and purpose {} with validity {}", request.getClientId(),
				request.getPurpose(), request.getValidity());
		final ValidityConfigModel model = builder.getConfig(request.getClientId(), request.getPurpose());
		if (ObjectUtils.isEmpty(model)) {
			log.error("Client {} Purpose {} Does not Exist therefore can not be edited", request.getClientId(),
					request.getPurpose());
			throw new FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_DOES_NOT_EXIST);
		} else if(request.getValidity() < 0) {
			log.error("Validity Can not be -ve", request.getValidity());
			throw new ValidationException(ErrorCodeAndMessage.VALIDITY_CAN_NOT_BE_NULL);
		}
		final int updateRecord = builder.updateRecord(request.getClientId(), request.getPurpose(),
				request.getValidity());
		return new ServiceResponse<>(updateRecord, true);
	}


	@Logged
	@Timed
	@Marked
	@DeleteMapping(ApiUrl.DELETE_CLIENT_PURPOSE)
	public ServiceResponse<Boolean> deletePurposeClientRecord(@RequestParam(CLIENT_KEY) @NotBlank String clientId,
			@RequestParam(PURPOSE_KEY) @NotBlank String purpose) {
		log.info("Deletion request for client {} and purpose {} with validity {}", clientId, purpose);
		final ValidityConfigModel model = builder.getConfig(clientId, purpose);
		if (ObjectUtils.isEmpty(model)) {
			log.error("Client {} Purpose {} Does not Exist therefore can not be edited", clientId, purpose);
			throw new FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_DOES_NOT_EXIST);
		}
		builder.deletePurposeForClient(clientId, purpose);
		return new ServiceResponse<>(true, true);
	}

	@Logged
	@Timed
	@Marked
	@GetMapping(ApiUrl.VALIDITY_FOR_CLIENT_AND_PURPOSE)
	public ServiceResponse<Integer> getValidityForClientAndPurpose(@RequestParam(CLIENT_KEY) @NotBlank String clientId,
			@RequestParam(PURPOSE_KEY) @NotBlank String purpose) {
		log.info("Get Validity request for client {} and purpose {} with validity {}", clientId, purpose);
		final ValidityConfigModel model = builder.getConfig(clientId, purpose);
		if (ObjectUtils.isEmpty(model)) {
			log.error("Client {} Purpose {} Record Does not Exist", clientId, purpose);
			throw new FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_DOES_NOT_EXIST);
		}
		return new ServiceResponse<>(model.getValidity(), true);
	}

	@Logged
	@Timed
	@Marked
	@GetMapping(ApiUrl.ALL_PURPOSE_FOR_CLIENT)
	public ServiceResponse<GetAllPurposeForClientResponse> getAllPurposeForClient(
			@RequestParam(CLIENT_KEY) @NotBlank String clientId) {
		log.info("GetAllPurposeForClient for client {} ", clientId);
		final List<ValidityConfigModel> models = builder.getAllRecordForClient(clientId);
		if (CollectionUtils.isEmpty(models)) {
			log.error("For Client {} Records Does not Exist", clientId);
			throw new  FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_DOES_NOT_EXIST);
		}
		final List<String> purposes = models
				.stream()
				.map(ValidityConfigModel::getPurpose)
				.collect(Collectors.toList());
		return new ServiceResponse<>(GetAllPurposeForClientResponse.builder().purposes(purposes).build(), true);
	}

	@Logged
	@Timed
	@Marked
	@GetMapping(ApiUrl.ALL_CLIENT_FOR_PURPOSE)
	public ServiceResponse<GetAllClientForPurposeResponse> getAllClientForPurpose(
			@RequestParam(PURPOSE_KEY) @NotBlank String purpose) {
		log.info("GetAllPurposeForClient for purpose {} ", purpose);
		final List<ValidityConfigModel> models = builder.getAllRecordForPurpose(purpose);
		if (CollectionUtils.isEmpty(models)) {
			log.error("For purpose {} Records Does not Exist", purpose);
			throw new FCBadRequestException(ErrorCodeAndMessage.CLIENT_PURPOSE_DOES_NOT_EXIST);
		}
		final List<String> clientIds = models
				.stream()
				.map(ValidityConfigModel::getClientId)
				.collect(Collectors.toList());
		return new ServiceResponse<>(GetAllClientForPurposeResponse.builder().clientIds(clientIds).build(), true);
	}
}
