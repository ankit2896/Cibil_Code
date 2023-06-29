package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.Borrower;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.assetresponse.IdentifierPartition;
import com.freecharge.cibil.model.assetresponse.Name;
import com.freecharge.cibil.model.enums.Gender;
import com.freecharge.cibil.model.enums.IdentificationType;
import com.freecharge.cibil.model.pojo.IdentificationInformation;
import com.freecharge.cibil.model.pojo.PersonalInformation;
import com.freecharge.cibil.model.pojo.PersonalInformationData;
import com.freecharge.cibil.utils.DateUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonalInfoBuilder {

	public static PersonalInformation build() {
		return null;
	}

	public static PersonalInformation buildPersonalInformation(
			@NonNull final GetCustomerAssetsSuccess getCustomerAssetsSuccess,
			@NonNull final Date fetchDate, @NonNull final String imsId, @NonNull final String txnId) {
		final Borrower borrower = getCustomerAssetsSuccess.getAsset().get(0).getTrueLinkCreditReport().getBorrower().get(0);
		List<IdentifierPartition> identifierPartitions = new ArrayList<>();
		borrower.getIdentifierPartition().stream().forEach(identifierPartitions::addAll);
		final PersonalInformationData personalInformationData = PersonalInformationData
				.builder()
				.dateOfBirth(getBirthDate(borrower))
				.fullName(buildName(borrower.getBorrowerName().get(0).getName()))
				.gender(Gender.valueOf(borrower.getGender().toUpperCase()))
				.identificationInformations(
						PersonalInfoBuilder.buildIdentificationInformation(identifierPartitions))
				.build();

		return PersonalInformation.builder()
				.personalInformationData(personalInformationData)
				.fetchDate(fetchDate)
				.imsId(imsId)
				.txnId(txnId)
				.build();
	}

	private static List<IdentificationInformation> buildIdentificationInformation(
			List<IdentifierPartition> identifierPartition) {
		final List<IdentificationInformation> identificationInformationList = new ArrayList<>();
        identifierPartition.forEach(e -> {
            IdentificationInformation identificationInformation = IdentificationInformation.builder()
                    .identificationNumber(e.getID().getIdentifierId())
                    .issueDate(DateUtils.getDateFromString(e.getDateIssued()))
                    .expirationDate(DateUtils.getDateFromString(e.getID().getExpirationDate()))
                    .type(IdentificationType.valueOfLabel(e.getID().getIdentifierName()))
                    .build();
            identificationInformationList.add(identificationInformation);
        });
        return identificationInformationList;
    }

	private static String buildName(@NonNull final Name name) {
		return name.getForename().get(0) + " " + StringUtils.join(name.getSurname(), ' ');
	}

	private static Date getBirthDate(Borrower borrower) {
		if (Optional.ofNullable(borrower.getBirth()).isPresent()) {
			return DateUtils.getDateFromString(borrower.getBirth().get(0).getDate());
		}
		return null;
	}

}
