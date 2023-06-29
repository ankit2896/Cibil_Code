package com.freecharge.cibil.builder;

import com.freecharge.cibil.model.assetresponse.Borrower;
import com.freecharge.cibil.model.assetresponse.BorrowerAddress;
import com.freecharge.cibil.model.assetresponse.EmailAddress;
import com.freecharge.cibil.model.assetresponse.GetCustomerAssetsSuccess;
import com.freecharge.cibil.model.enums.*;
import com.freecharge.cibil.model.pojo.*;
import com.freecharge.cibil.utils.DateUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContactInformationBuilder {

	private static Map<Integer, String> pincodeCityMap = null;

	public static void assignPincodeCityMap(Map<Integer, String> pinCodeCityMap) {
		pincodeCityMap = pinCodeCityMap;
	}

	public static ContactInformation buildContactInformation(
			@NonNull final GetCustomerAssetsSuccess getCustomerAssetsSuccess, @NonNull final Date fetchDate,
			@NonNull final String imsId, @NonNull final String txnId) {
		final Borrower borrower = getCustomerAssetsSuccess.getAsset().get(0).getTrueLinkCreditReport().getBorrower().get(0);
		final ContactInformationData contactInformationData = ContactInformationData.builder()
				.addressInformations(buildAddressInfoList(borrower.getBorrowerAddressList()))
				.phoneInformations(buildPhoneInfoList(borrower))
				.emailInformation(buildEmailInformation(borrower.getEmailAddress().get(0))).build();

		return ContactInformation
				.builder()
				.contactInformationData(contactInformationData)
				.fetchDate(fetchDate)
				.imsId(imsId)
				.txnId(txnId)
				.build();
	}

	private static List<AddressInformation> buildAddressInfoList(
			@NonNull final List<BorrowerAddress> borrowerAddressList) {
		final List<AddressInformation> addressInformationList = new ArrayList<>();
		borrowerAddressList.forEach(e -> {
			AddressInformation addressInformation = AddressInformation.builder().address(buildAddress(e))
					.addressCatagory(getAddressCategory(e))
					.dateReported(DateUtils.getDateFromString(e.getDateReported()))
					.addressOwnership(getAddressStatus(e)).build();
			addressInformationList.add(addressInformation);
		});
		return addressInformationList;
	}

	private static Address buildAddress(BorrowerAddress borrowerAddress) {
		return Address.builder().streetAddress(borrowerAddress.getCreditAddress().getStreetAddress().get(0))
				.postalCode(Integer.toString(borrowerAddress.getCreditAddress().getPostalCode()))
				.city(pincodeCityMap.get(borrowerAddress.getCreditAddress().getPostalCode()))
				.addressType(AddressType.nameOfValue(Integer.parseInt(borrowerAddress.getDwelling().getSymbol())))
				.region(Region.nameOfValue(Integer.parseInt(borrowerAddress.getCreditAddress().getRegion()))).build();
	}

	private static List<PhoneInformation> buildPhoneInfoList(@NonNull final Borrower borrower) {
		final List<PhoneInformation> phoneInformationList = new ArrayList<>();
		borrower.getBorrowerTelephone().forEach(e -> {
			PhoneInformation phoneInformation = PhoneInformation.builder().number(e.getPhoneNumber().getNumber())
					.extension(e.getPhoneNumber().getExtension())
					.type(PhoneType.nameOfSymbol(e.getTelephoneType().getSymbol())).build();
			phoneInformationList.add(phoneInformation);
		});

		return phoneInformationList;
	}

	private static EmailInformation buildEmailInformation(EmailAddress emailAddress) {
		return EmailInformation.builder().email(emailAddress.getEmail()).build();
	}

	private static AddressOwnership getAddressStatus(BorrowerAddress borrowerAddress) {
		if (borrowerAddress.getAddressOwnership() == null
				|| borrowerAddress.getAddressOwnership().getSymbol().isEmpty()) {
			return null;
		}
		int symbol = Integer.parseInt(borrowerAddress.getAddressOwnership().getSymbol());
		return AddressOwnership.valueOfLabel(symbol);
	}

	private static AddressCategory getAddressCategory(BorrowerAddress borrowerAddress) {
		return Optional.ofNullable(borrowerAddress.getDwelling()).isPresent()
				? AddressCategory.valueOfLabel(Integer.parseInt(borrowerAddress.getDwelling().getSymbol()))
				: null;
	}
}
