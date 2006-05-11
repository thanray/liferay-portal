/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.service.impl;

import com.liferay.counter.service.spring.CounterServiceUtil;
import com.liferay.portal.AddressCityException;
import com.liferay.portal.AddressStreetException;
import com.liferay.portal.AddressZipException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Country;
import com.liferay.portal.model.ListType;
import com.liferay.portal.model.Region;
import com.liferay.portal.model.User;
import com.liferay.portal.service.persistence.AddressUtil;
import com.liferay.portal.service.persistence.UserUtil;
import com.liferay.portal.service.spring.AddressLocalService;
import com.liferay.portal.service.spring.CountryServiceUtil;
import com.liferay.portal.service.spring.ListTypeServiceUtil;
import com.liferay.portal.service.spring.RegionServiceUtil;
import com.liferay.util.Validator;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * <a href="AddressLocalServiceImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class AddressLocalServiceImpl implements AddressLocalService {

	public Address addAddress(
			String userId, String className, String classPK, String street1,
			String street2, String street3, String city, String zip,
			String regionId, String countryId, String typeId, boolean mailing,
			boolean primary)
		throws PortalException, SystemException {

		User user = UserUtil.findByPrimaryKey(userId);
		Date now = new Date();

		validate(
			null, user.getCompanyId(), className, classPK, street1, city, zip,
			regionId, countryId, typeId, mailing, primary);

		String addressId = Long.toString(CounterServiceUtil.increment(
			Address.class.getName()));

		Address address = AddressUtil.create(addressId);

		address.setCompanyId(user.getCompanyId());
		address.setUserId(user.getUserId());
		address.setUserName(user.getFullName());
		address.setCreateDate(now);
		address.setModifiedDate(now);
		address.setClassName(className);
		address.setClassPK(classPK);
		address.setStreet1(street1);
		address.setStreet2(street2);
		address.setStreet3(street3);
		address.setCity(city);
		address.setZip(zip);
		address.setRegionId(regionId);
		address.setCountryId(countryId);
		address.setTypeId(typeId);
		address.setMailing(mailing);
		address.setPrimary(primary);

		AddressUtil.update(address);

		return address;
	}

	public void deleteAddress(String addressId)
		throws PortalException, SystemException {

		AddressUtil.remove(addressId);
	}

	public void deleteAddresses(
			String companyId, String className, String classPK)
		throws SystemException {

		AddressUtil.removeByC_C_C(companyId, className, classPK);
	}

	public Address getAddress(String addressId)
		throws PortalException, SystemException {

		return AddressUtil.findByPrimaryKey(addressId);
	}

	public List getAddresses(String companyId, String className, String classPK)
		throws SystemException {

		return AddressUtil.findByC_C_C(companyId, className, classPK);
	}

	public Address updateAddress(
			String addressId, String street1, String street2, String street3,
			String city, String zip, String regionId, String countryId,
			String typeId, boolean mailing, boolean primary)
		throws PortalException, SystemException {

		validate(
			addressId, null, null, null, street1, city, zip, regionId,
			countryId, typeId, mailing, primary);

		Address address = AddressUtil.findByPrimaryKey(addressId);

		address.setModifiedDate(new Date());
		address.setStreet1(street1);
		address.setStreet2(street2);
		address.setStreet3(street3);
		address.setCity(city);
		address.setZip(zip);
		address.setRegionId(regionId);
		address.setCountryId(countryId);
		address.setTypeId(typeId);
		address.setMailing(mailing);
		address.setPrimary(primary);

		AddressUtil.update(address);

		return address;
	}

	protected void validate(
			String addressId, String companyId, String className,
			String classPK, String street1, String city, String zip,
			String regionId, String countryId, String typeId, boolean mailing,
			boolean primary)
		throws PortalException, SystemException {

		if (Validator.isNull(street1)) {
			throw new AddressStreetException();
		}
		else if (Validator.isNull(city)) {
			throw new AddressCityException();
		}
		else if (Validator.isNull(zip)) {
			throw new AddressZipException();
		}

		Region region = RegionServiceUtil.getRegion(regionId);

		Country country = CountryServiceUtil.getCountry(countryId);

		if (addressId != null) {
			Address address = AddressUtil.findByPrimaryKey(addressId);

			companyId = address.getCompanyId();
			className = address.getClassName();
			classPK = address.getClassPK();
		}

		ListTypeServiceUtil.validate(typeId, className + ListType.ADDRESS);

		validate(addressId, companyId, className, classPK, mailing, primary);
	}

	protected void validate(
			String addressId, String companyId, String className,
			String classPK, boolean mailing, boolean primary)
		throws PortalException, SystemException {

		// Check to make sure there isn't another address with the same company
		// id, class name, and class pk that also has mailing set to true

		if (mailing) {
			Iterator itr = AddressUtil.findByC_C_C_M(
				companyId, className, classPK, mailing).iterator();

			while (itr.hasNext()) {
				Address address = (Address)itr.next();

				if ((addressId == null) ||
					(!address.getAddressId().equals(addressId))) {

					address.setMailing(false);

					AddressUtil.update(address);
				}
			}
		}

		// Check to make sure there isn't another address with the same company
		// id, class name, and class pk that also has primary set to true

		if (primary) {
			Iterator itr = AddressUtil.findByC_C_C_P(
				companyId, className, classPK, primary).iterator();

			while (itr.hasNext()) {
				Address address = (Address)itr.next();

				if ((addressId == null) ||
					(!address.getAddressId().equals(addressId))) {

					address.setPrimary(false);

					AddressUtil.update(address);
				}
			}
		}
	}

}