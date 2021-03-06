/*******************************************************************************
 * OpenCPS is the open source Core Public Services software
 * Copyright (C) 2016-present OpenCPS community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package org.opencps.dossiermgt.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencps.dossiermgt.NoSuchDossierFileException;
import org.opencps.dossiermgt.NoSuchDossierStatusException;
import org.opencps.dossiermgt.model.Dossier;
import org.opencps.dossiermgt.model.DossierFile;
import org.opencps.dossiermgt.model.DossierPart;
import org.opencps.dossiermgt.model.FileGroup;
import org.opencps.dossiermgt.service.DossierFileLocalServiceUtil;
import org.opencps.dossiermgt.service.base.DossierFileLocalServiceBaseImpl;
import org.opencps.processmgt.model.WorkflowOutput;
import org.opencps.util.PortletConstants;

import com.itextpdf.text.pdf.PdfStructTreeController.returnType;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.OrderByComparatorFactory;
import com.liferay.portal.kernel.util.OrderByComparatorFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;

/**
 * The implementation of the dossier file local service.
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link org.opencps.dossiermgt.service.DossierFileLocalService} interface.
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 *
 * @author trungnt
 * @see org.opencps.dossiermgt.service.base.DossierFileLocalServiceBaseImpl
 * @see org.opencps.dossiermgt.service.DossierFileLocalServiceUtil
 */

public class DossierFileLocalServiceImpl extends
		DossierFileLocalServiceBaseImpl {

	/*
	 * NOTE FOR DEVELOPERS: Never reference this interface directly. Always use
	 * {@link org.opencps.dossiermgt.service.DossierFileLocalServiceUtil} to
	 * access the dossier file local service.
	 */

	/**
	 * @param userId
	 * @param dossierId
	 * @param dossierPartId
	 * @param templateFileNo
	 * @param groupName
	 * @param fileGroupId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param displayName
	 * @param formData
	 * @param fileEntryId
	 * @param dossierFileMark
	 * @param dossierFileType
	 * @param dossierFileNo
	 * @param dossierFileDate
	 * @param original
	 * @param syncStatus
	 * @param version
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addDossierFile(long userId, long dossierId,
			long dossierPartId, String templateFileNo, String groupName,
			long fileGroupId, long groupDossierPartId, long ownerUserId,
			long ownerOrganizationId, String displayName, String formData,
			long fileEntryId, int dossierFileMark, int dossierFileType,
			String dossierFileNo, Date dossierFileDate, int original,
			int syncStatus, ServiceContext serviceContext)
			throws SystemException, PortalException {
		_log.info("STARTTTTTT ADDD DOSSIER FILE");
		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		int version = 0;

		// Add new FileGroup
		if (Validator.isNotNull(groupName) && fileGroupId == 0) {
			FileGroup fileGroup = fileGroupLocalService.addFileGroup(
					ownerUserId, dossierId, dossierPartId, groupName,
					syncStatus, serviceContext);

			fileGroupId = fileGroup.getFileGroupId();
		}

		dossierFile.setUserId(userId);
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(displayName);
		dossierFile.setDossierFileDate(dossierFileDate);
		dossierFile.setDossierFileMark(dossierFileMark);
		dossierFile.setDossierFileNo(dossierFileNo);
		dossierFile.setDossierFileType(dossierFileType);
		dossierFile.setDossierId(dossierId);
		dossierFile.setDossierPartId(dossierPartId);
		dossierFile.setFileEntryId(fileEntryId);
		dossierFile.setFormData(formData);
		dossierFile.setGroupFileId(fileGroupId);
		dossierFile.setOriginal(original);
		dossierFile.setOwnerUserId(ownerUserId);
		dossierFile.setSyncStatus(syncStatus);
		dossierFile.setOwnerOrganizationId(ownerOrganizationId);

		dossierFile.setTemplateFileNo(templateFileNo);

		if (fileGroupId > 0) {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP_GF(
					dossierId, dossierPartId, fileGroupId) + 1;
		} else {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP(
					dossierId, dossierPartId) + 1;
		}

		dossierFile.setVersion(version);

		DossierFile curVersion = null;

		try {
			if (fileGroupId > 0) {
				curVersion = dossierFileLocalService
						.getDossierFileInUseByGroupFileId(dossierId,
								dossierPartId, fileGroupId);
			} else {
				curVersion = dossierFileLocalService.getDossierFileInUse(
						dossierId, dossierPartId);
			}
		} catch (Exception e) {
		}

		if (curVersion != null) {
			dossierFile.setOid(curVersion.getOid());
			dossierFileLocalService.removeDossierFile(curVersion
					.getDossierFileId());
		} else {
			dossierFile.setOid(PortalUUIDUtil.generate());
		}

		dossierFile = dossierFilePersistence.update(dossierFile);

		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param oldDossierFileId
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param is
	 * @param size
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addDossierFile(long oldDossierFileId, long folderId,
			String sourceFileName, String mimeType, String title,
			String description, String changeLog, InputStream is, long size,
			ServiceContext serviceContext) throws SystemException,
			PortalException {
		_log.info("STARTTTTTT ADDD DOSSIER FILE");
		DossierFile oldDossierFile = dossierFilePersistence
				.findByPrimaryKey(oldDossierFileId);

		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		dossierFile.setUserId(oldDossierFile.getUserId());
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(oldDossierFile.getDisplayName());
		dossierFile.setDossierFileDate(oldDossierFile.getDossierFileDate());
		dossierFile.setDossierFileMark(oldDossierFile.getDossierFileMark());
		dossierFile.setDossierFileNo(oldDossierFile.getDossierFileNo());
		dossierFile.setDossierFileType(oldDossierFile.getDossierFileType());
		dossierFile.setDossierId(oldDossierFile.getDossierId());
		dossierFile.setDossierPartId(oldDossierFile.getDossierPartId());

		dossierFile.setFormData(oldDossierFile.getFormData());
		dossierFile.setGroupFileId(oldDossierFile.getGroupFileId());
		dossierFile.setOriginal(oldDossierFile.getOriginal());
		dossierFile.setOwnerUserId(oldDossierFile.getOwnerUserId());

		dossierFile.setOwnerOrganizationId(oldDossierFile
				.getOwnerOrganizationId());

		dossierFile.setTemplateFileNo(oldDossierFile.getTemplateFileNo());

		dossierFile.setVersion(oldDossierFile.getVersion() + 1);

		dossierFile.setOid(oldDossierFile.getOid());

		// Add file
		FileEntry fileEntry = dlAppService.addFileEntry(
				serviceContext.getScopeGroupId(), folderId, sourceFileName,
				mimeType, getFileName(sourceFileName), description, changeLog,
				is, size, serviceContext);

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());

		dossierFileLocalService.removeDossierFile(oldDossierFile
				.getDossierFileId());

		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param oldDossierFileId
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param bytes
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addDossierFile(long oldDossierFileId, long folderId,
			String sourceFileName, String mimeType, String title,
			String description, String changeLog, byte[] bytes,
			ServiceContext serviceContext) throws SystemException,
			PortalException {
		_log.info("STARTTTTTT ADDD DOSSIER FILE");
		DossierFile oldDossierFile = dossierFilePersistence
				.findByPrimaryKey(oldDossierFileId);

		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		dossierFile.setUserId(oldDossierFile.getUserId());
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(oldDossierFile.getDisplayName());
		dossierFile.setDossierFileDate(oldDossierFile.getDossierFileDate());
		dossierFile.setDossierFileMark(oldDossierFile.getDossierFileMark());
		dossierFile.setDossierFileNo(oldDossierFile.getDossierFileNo());
		dossierFile.setDossierFileType(oldDossierFile.getDossierFileType());
		dossierFile.setDossierId(oldDossierFile.getDossierId());
		dossierFile.setDossierPartId(oldDossierFile.getDossierPartId());

		dossierFile.setFormData(oldDossierFile.getFormData());
		dossierFile.setGroupFileId(oldDossierFile.getGroupFileId());
		dossierFile.setOriginal(oldDossierFile.getOriginal());
		dossierFile.setOwnerUserId(oldDossierFile.getOwnerUserId());

		dossierFile.setOwnerOrganizationId(oldDossierFile
				.getOwnerOrganizationId());

		dossierFile.setTemplateFileNo(oldDossierFile.getTemplateFileNo());

		dossierFile.setVersion(oldDossierFile.getVersion() + 1);

		dossierFile.setOid(oldDossierFile.getOid());

		// Add file
		FileEntry fileEntry = dlAppService.addFileEntry(
				serviceContext.getScopeGroupId(), folderId, sourceFileName,
				mimeType, getFileName(sourceFileName), description, changeLog,
				bytes, serviceContext);

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());

		dossierFileLocalService.removeDossierFile(oldDossierFile
				.getDossierFileId());

		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param userId
	 * @param dossierId
	 * @param dossierPartId
	 * @param templateFileNo
	 * @param groupName
	 * @param fileGroupId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param displayName
	 * @param formData
	 * @param fileEntryId
	 * @param dossierFileMark
	 * @param dossierFileType
	 * @param dossierFileNo
	 * @param dossierFileDate
	 * @param original
	 * @param syncStatus
	 * @param version
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param is
	 * @param size
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addDossierFile(long userId, long dossierId,
			long dossierPartId, String templateFileNo, String groupName,
			long fileGroupId, long groupDossierPartId, long ownerUserId,
			long ownerOrganizationId, String displayName, String formData,
			long fileEntryId, int dossierFileMark, int dossierFileType,
			String dossierFileNo, Date dossierFileDate, int original,
			int syncStatus, long folderId, String sourceFileName,
			String mimeType, String title, String description,
			String changeLog, InputStream is, long size, int signCheck, 
			String signInfo, ServiceContext serviceContext) throws SystemException,
			PortalException {
			

		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		int version = 1;

		// Add new FileGroup
		if (Validator.isNotNull(groupName) && fileGroupId == 0
				&& groupDossierPartId > 0) {
			FileGroup fileGroup = fileGroupLocalService.addFileGroup(
					ownerUserId, dossierId, groupDossierPartId, groupName,
					syncStatus, serviceContext);

			fileGroupId = fileGroup.getFileGroupId();
		}

		dossierFile.setUserId(userId);
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(displayName);
		dossierFile.setDossierFileDate(dossierFileDate);
		dossierFile.setDossierFileMark(dossierFileMark);
		dossierFile.setDossierFileNo(dossierFileNo);
		dossierFile.setDossierFileType(dossierFileType);
		dossierFile.setDossierId(dossierId);
		dossierFile.setDossierPartId(dossierPartId);
		dossierFile.setSyncStatus(syncStatus);
		dossierFile.setFormData(formData);
		dossierFile.setGroupFileId(fileGroupId);
		dossierFile.setOriginal(original);
		dossierFile.setOwnerUserId(ownerUserId);
		dossierFile.setSignCheck(signCheck);
		dossierFile.setSignInfo(signInfo);

		dossierFile.setOwnerOrganizationId(ownerOrganizationId);

		dossierFile.setTemplateFileNo(templateFileNo);

		if (fileGroupId > 0) {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP_GF(
					dossierId, dossierPartId, fileGroupId) + 1;
		} else {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP(
					dossierId, dossierPartId) + 1;
		}

		dossierFile.setVersion(version);

		// Add file
		FileEntry fileEntry = dlAppService.addFileEntry(
				serviceContext.getScopeGroupId(), folderId, sourceFileName,
				mimeType, getFileName(sourceFileName), description, changeLog,
				is, size, serviceContext);

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());

		DossierFile curVersion = null;

		try {
			DossierPart dossierPart = dossierPartLocalService
					.getDossierPart(dossierPartId);

			if (dossierPart.getPartType() != PortletConstants.DOSSIER_PART_TYPE_OTHER
					&& dossierPart.getPartType() != PortletConstants.DOSSIER_PART_TYPE_MULTIPLE_RESULT) {
				if (fileGroupId > 0) {
					curVersion = dossierFileLocalService
							.getDossierFileInUseByGroupFileId(dossierId,
									dossierPartId, fileGroupId);
				} else {
					curVersion = dossierFileLocalService.getDossierFileInUse(
							dossierId, dossierPartId);
				}
			}

		} catch (Exception e) {
		}

		if (curVersion != null) {
			dossierFile.setOid(curVersion.getOid());
			dossierFileLocalService.removeDossierFile(curVersion
					.getDossierFileId());
		} else {
			dossierFile.setOid(PortalUUIDUtil.generate());
		}

		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);
		
		return dossierFile;
	}

	/**
	 * @param userId
	 * @param dossierId
	 * @param dossierPartId
	 * @param templateFileNo
	 * @param groupName
	 * @param fileGroupId
	 * @param groupDossierPartId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param displayName
	 * @param formData
	 * @param dossierFileMark
	 * @param dossierFileType
	 * @param dossierFileNo
	 * @param dossierFileDate
	 * @param original
	 * @param syncStatus
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param bytes
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addDossierFile(long userId, long dossierId,
			long dossierPartId, String templateFileNo, String groupName,
			long fileGroupId, long groupDossierPartId, long ownerUserId,
			long ownerOrganizationId, String displayName, String formData,
			int dossierFileMark, int dossierFileType, String dossierFileNo,
			Date dossierFileDate, int original, int syncStatus, long folderId,
			String sourceFileName, String mimeType, String title,
			String description, String changeLog, byte[] bytes,
			ServiceContext serviceContext) throws SystemException,
			PortalException {
		_log.info("STARTTTTTT ADDD DOSSIER FILE");
		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		int version = 1;

		// Add new FileGroup
		if (Validator.isNotNull(groupName) && fileGroupId == 0
				&& groupDossierPartId > 0) {
			FileGroup fileGroup = fileGroupLocalService.addFileGroup(
					ownerUserId, dossierId, groupDossierPartId, groupName,
					syncStatus, serviceContext);

			fileGroupId = fileGroup.getFileGroupId();
		}

		dossierFile.setUserId(userId);
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(displayName);
		dossierFile.setDossierFileDate(dossierFileDate);
		dossierFile.setDossierFileMark(dossierFileMark);
		dossierFile.setDossierFileNo(dossierFileNo);
		dossierFile.setDossierFileType(dossierFileType);
		dossierFile.setDossierId(dossierId);
		dossierFile.setDossierPartId(dossierPartId);
		dossierFile.setSyncStatus(syncStatus);
		dossierFile.setFormData(formData);
		dossierFile.setGroupFileId(fileGroupId);
		dossierFile.setOriginal(original);
		dossierFile.setOwnerUserId(ownerUserId);

		dossierFile.setOwnerOrganizationId(ownerOrganizationId);

		dossierFile.setTemplateFileNo(templateFileNo);

		if (fileGroupId > 0) {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP_GF(
					dossierId, dossierPartId, fileGroupId) + 1;
		} else {
			version = DossierFileLocalServiceUtil.countDossierFileByDID_DP(
					dossierId, dossierPartId) + 1;
		}

		dossierFile.setVersion(version);

		// Add file

		User user = UserLocalServiceUtil.getUser(serviceContext.getUserId());

		PermissionChecker permissionChecker;

		try {
			permissionChecker = PermissionCheckerFactoryUtil.create(user);
			PermissionThreadLocal.setPermissionChecker(permissionChecker);
		} catch (Exception e) {
		}

		FileEntry fileEntry = dlAppLocalService.addFileEntry(
				serviceContext.getUserId(), serviceContext.getScopeGroupId(),
				folderId, sourceFileName, mimeType,
				getFileName(sourceFileName), description, changeLog, bytes,
				serviceContext);
		/*
		 * FileEntry fileEntry = dlAppServiceUtil.addFileEntry(
		 * serviceContext.getScopeGroupId(), folderId, sourceFileName, mimeType,
		 * displayName + StringPool.DASH + dossierFileId + StringPool.DASH +
		 * version + StringPool.DASH + System.currentTimeMillis(), description,
		 * changeLog, bytes, serviceContext);
		 */

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());

		DossierFile curVersion = null;

		try {
			DossierPart dossierPart = dossierPartLocalService
					.getDossierPart(dossierPartId);

			if (dossierPart.getPartType() != PortletConstants.DOSSIER_PART_TYPE_OTHER
					&& dossierPart.getPartType() != PortletConstants.DOSSIER_PART_TYPE_MULTIPLE_RESULT) {
				if (fileGroupId > 0) {
					curVersion = dossierFileLocalService
							.getDossierFileInUseByGroupFileId(dossierId,
									dossierPartId, fileGroupId);
				} else {
					curVersion = dossierFileLocalService.getDossierFileInUse(
							dossierId, dossierPartId);
				}
			}

		} catch (Exception e) {
		}

		if (curVersion != null) {
			dossierFile.setOid(curVersion.getOid());
			dossierFileLocalService.removeDossierFile(curVersion
					.getDossierFileId());
		} else {
			dossierFile.setOid(PortalUUIDUtil.generate());
		}

		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param oldDossierFileId
	 * @param isSigned
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param is
	 * @param size
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile addSignDossierFile(long oldDossierFileId,
			boolean isSigned, long folderId, String sourceFileName,
			String mimeType, String title, String description,
			String changeLog, InputStream is, long size,
			ServiceContext serviceContext) throws SystemException,
			PortalException {

		DossierFile oldDossierFile = dossierFilePersistence
				.findByPrimaryKey(oldDossierFileId);

		long dossierFileId = counterLocalService.increment(DossierFile.class
				.getName());
		DossierFile dossierFile = dossierFilePersistence.create(dossierFileId);

		Date now = new Date();

		dossierFile.setUserId(oldDossierFile.getUserId());
		dossierFile.setGroupId(serviceContext.getScopeGroupId());
		dossierFile.setCompanyId(serviceContext.getCompanyId());
		dossierFile.setCreateDate(now);
		dossierFile.setModifiedDate(now);
		dossierFile.setDisplayName(oldDossierFile.getDisplayName());
		dossierFile.setDossierFileDate(oldDossierFile.getDossierFileDate());
		dossierFile.setDossierFileMark(oldDossierFile.getDossierFileMark());
		dossierFile.setDossierFileNo(oldDossierFile.getDossierFileNo());
		dossierFile.setDossierFileType(oldDossierFile.getDossierFileType());
		dossierFile.setDossierId(oldDossierFile.getDossierId());
		dossierFile.setDossierPartId(oldDossierFile.getDossierPartId());

		dossierFile.setFormData(oldDossierFile.getFormData());
		dossierFile.setGroupFileId(oldDossierFile.getGroupFileId());
		dossierFile.setOriginal(oldDossierFile.getOriginal());
		dossierFile.setOwnerUserId(oldDossierFile.getOwnerUserId());

		dossierFile.setOwnerOrganizationId(oldDossierFile
				.getOwnerOrganizationId());

		dossierFile.setTemplateFileNo(oldDossierFile.getTemplateFileNo());

		dossierFile.setVersion(oldDossierFile.getVersion() + 1);

		dossierFile.setOid(oldDossierFile.getOid());

		dossierFile.setSigned(isSigned);

		// Add file
		FileEntry fileEntry = dlAppService.addFileEntry(
				serviceContext.getScopeGroupId(), folderId, sourceFileName,
				mimeType, getFileName(sourceFileName), description, changeLog,
				is, size, serviceContext);

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());

		dossierFileLocalService.removeDossierFile(oldDossierFile
				.getDossierFileId());

		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFileByDID_DP(long dossierId, long dossierPartId)
			throws SystemException {

		return dossierFilePersistence.countByDID_DP(dossierId, dossierPartId);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param removed
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFileByDID_DP_R(long dossierId, long dossierPartId,
			int removed) throws SystemException {

		return dossierFilePersistence.countByDID_DP_R(dossierId, dossierPartId,
				removed);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param groupFileId
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFileByDID_DP_GF(long dossierId, long dossierPartId,
			long groupFileId) throws SystemException {

		return dossierFilePersistence.countByDID_DP_GF(dossierId,
				dossierPartId, groupFileId);
	}

	/**
	 * @param groupId
	 * @param keyword
	 * @param templateFileNo
	 * @param removed
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFile(long groupId, long ownerUserId,
			long ownerOrganizationId, String keyword, String templateFileNo,
			int removed) throws SystemException {

		return dossierFileFinder.countDossierFile(groupId, ownerUserId,
				ownerOrganizationId, keyword, templateFileNo, removed);
	}

	/**
	 * @param groupId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param keyword
	 * @param templateFileNo
	 * @param removed
	 * @param partType
	 * @param original
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFileAdvance(long groupId, long ownerUserId,
			long ownerOrganizationId, String keyword, String templateFileNo,
			int removed, int partType, int original) throws SystemException {

		return dossierFileFinder.countDossierFileAdvance(groupId, ownerUserId,
				ownerOrganizationId, keyword, templateFileNo, removed,
				partType, original);
	}

	/**
	 * @param groupId
	 * @param keyword
	 * @param dossierTemplateId
	 * @param fileEntryId
	 * @param onlyViewFileResult
	 * @return
	 * @throws SystemException
	 */
	public int countDossierFile(long groupId, String keyword,

			long dossierTemplateId, long fileEntryId, int syncStatus,
			int removed, boolean onlyViewFileResult) throws SystemException {

		return dossierFileFinder.countDossierFile(groupId, keyword,
				dossierTemplateId, fileEntryId, syncStatus, removed,
				onlyViewFileResult);

	}

	/**
	 * @param dossierFileId
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void removeDossierFile(long dossierFileId) throws PortalException,
			SystemException {

		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		DossierFile dossierFile = DossierFileLocalServiceUtil
				.getDossierFile(dossierFileId);

		dossierFile.setRemoved(1);
		dossierFile.setModifiedDate(new Date());

		if (dossierFile.getSyncStatus() != PortletConstants.DOSSIER_FILE_SYNC_STATUS_SYNCSUCCESS) {
			dossierFile.setVersion(0);
		}

		indexer.reindex(dossierFile);

		dossierFilePersistence.update(dossierFile);

		dossierFilePersistence.clearCache();

		dossierFilePersistence.clearCache(dossierFile);
	}

	/**
	 * @param dossierFileId
	 * @param fileEntryId
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void deleteDossierFile(long dossierFileId, long fileEntryId)
			throws PortalException, SystemException {

		if (fileEntryId > 0) {
			dlFileEntryLocalService.deleteDLFileEntry(fileEntryId);
		}

		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		DossierFile dossierFile = DossierFileLocalServiceUtil
				.getDossierFile(dossierFileId);

		indexer.delete(dossierFile);

		dossierFilePersistence.remove(dossierFileId);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_DP(long dossierId,
			long dossierPartId) throws NoSuchDossierFileException,
			SystemException {

		return dossierFilePersistence.findByDID_DP(dossierId, dossierPartId);
	}
	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param syncStatus
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_SS_DP(
		long dossierId, long dossierPartId, int syncStatus)
		throws NoSuchDossierFileException, SystemException {

		return dossierFilePersistence.findByDID_SS_DP(dossierId, dossierPartId, syncStatus);
	}
	
	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param syncStatus
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */

	public int countDossierFileByDID_SS_DP(
		long dossierId, long dossierPartId, int syncStatus)
		throws NoSuchDossierFileException, SystemException {

		return dossierFilePersistence.countByDID_SS_DP(dossierId, dossierPartId, syncStatus);
	}
	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @return List
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_DP_R(long dossierId,
			long dossierPartId, int removed) throws NoSuchDossierFileException,
			SystemException {

		return dossierFilePersistence.findByDID_DP_R(dossierId, dossierPartId,
				removed);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param byComparator
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByD_DP_Config(long dossierId,
			long dossierPartId, OrderByComparator byComparator, int start,
			int end) throws NoSuchDossierFileException, SystemException {

		return dossierFilePersistence.findByDID_DP(dossierId, dossierPartId,
				start, end, byComparator);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public int countDossierFileByD_DP_Config(long dossierId, long dossierPartId)
			throws NoSuchDossierFileException, SystemException {

		return dossierFilePersistence.countByDID_DP(dossierId, dossierPartId);
	}

	/**
	 * @param fileGroupId
	 * @param dossierId
	 * @param syncStatus
	 * @param removed
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_GFID_SS_R(long fileGroupId,
			long dossierId, int syncStatus, int removed) throws SystemException {

		return dossierFilePersistence.findByDID_GFID_SS_R(fileGroupId,
				dossierId, syncStatus, removed);
	}

	/**
	 * @param dossierId
	 * @param syncStatus
	 * @param dossierPartId
	 * @param removed
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_SS_DPID_R(long dossierId,
			int syncStatus, long dossierPartId, int removed)
			throws SystemException {

		return dossierFilePersistence.findByDID_SS_DPID_R(dossierId,
				syncStatus, dossierPartId, removed);
	}

	/**
	 * Find all dossierFile in use(removed = 0)
	 * 
	 * @param dossierId
	 * @param groupFileId
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_GFID_R(long dossierId,
			long groupFileId, int removed) throws SystemException {

		return dossierFilePersistence.findByDID_GFID_R(dossierId, groupFileId,
				removed);
	}

	/**
	 * Find all dossierFile in use (removed = 0)
	 * 
	 * @param dossierId
	 * @return List<DossierFile>
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDossierId(long dossierId)
			throws SystemException {

		return dossierFilePersistence.findByDossierId(dossierId);
	}

	/**
	 * @param dossierId
	 * @param syncStatus
	 * @param removed
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> getDossierFileByDID_SS_R(long dossierId,
			int syncStatus, int removed) throws SystemException {

		return dossierFilePersistence.findByDID_SS_R(dossierId, syncStatus,
				removed);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @return DossierFile
	 * @throws PortalException
	 * @throws SystemException
	 */
	public DossierFile getDossierFileInUse(long dossierId, long dossierPartId)
			throws PortalException, SystemException {

		return dossierFilePersistence.findByDossierFileInUse(dossierId,
				dossierPartId, 0);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param syncStatus
	 * @return DossierFile
	 * @throws PortalException
	 * @throws SystemException
	 */
	public DossierFile getDossierFileInUse(long dossierId, long dossierPartId,
			int syncStatus) throws PortalException, SystemException {

		return dossierFilePersistence.findByDossierFileInUseSyncStatus(
				dossierId, dossierPartId, 0, syncStatus);
	}

	/**
	 * @param dossierId
	 * @param dossierPartId
	 * @param groupFileId
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public DossierFile getDossierFileInUseByGroupFileId(long dossierId,
			long dossierPartId, long groupFileId)
			throws NoSuchDossierFileException, SystemException {

		return dossierFilePersistence.findByDossierFileInUseByGroupFileId(
				dossierId, dossierPartId, groupFileId);
	}

	/**
	 * @param groupId
	 * @param keyword
	 * @param templateFileNo
	 * @param removed
	 * @param start
	 * @param end
	 * @param obc
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> searchDossierFile(long groupId, long ownerUserId,
			long ownerOrganizationId, String keyword, String templateFileNo,
			int removed, int start, int end, OrderByComparator obc)
			throws SystemException {

		return dossierFileFinder.searchDossierFile(groupId, ownerUserId,
				ownerOrganizationId, keyword, templateFileNo, removed, start,
				end, obc);
	}

	/**
	 * @param groupId
	 * @param keyword
	 * @param dossierTemplateId
	 * @param fileEntryId
	 * @param onlyViewFileResult
	 * @param start
	 * @param end
	 * @param obc
	 * @return
	 * @throws SystemException
	 */
	public List<DossierFile> searchDossierFile(long groupId, String keyword,

			long dossierTemplateId, long fileEntryId, int syncStatus,
			int removed, boolean onlyViewFileResult, int start, int end,
			OrderByComparator obc) throws SystemException {

		return dossierFileFinder.searchDossierFile(groupId, keyword,
				dossierTemplateId, fileEntryId, syncStatus, removed,
				onlyViewFileResult, start, end, obc);
	}

	/**
	 * @param groupId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param keyword
	 * @param templateFileNo
	 * @param removed
	 * @param partType
	 * @param original
	 * @param start
	 * @param end
	 * @param obc
	 * @return
	 * @throws SystemException
	 */
	public List searchDossierFileAdvance(long groupId, long ownerUserId,
			long ownerOrganizationId, String keyword, String templateFileNo,
			int removed, int partType, int original, int start, int end,
			OrderByComparator obc) throws SystemException {

		return dossierFileFinder.searchDossierFileAdvance(groupId, ownerUserId,
				ownerOrganizationId, keyword, templateFileNo, removed,
				partType, original, start, end, obc);
	}

	/**
	 * @param dossierFileId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param fileEntryId
	 * @param displayName
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile updateDossierFile(long dossierFileId, long ownerUserId,
			long ownerOrganizationId, long fileEntryId, String displayName)
			throws NoSuchDossierFileException, SystemException, PortalException {

		DossierFile dossierFile = dossierFilePersistence
				.findByPrimaryKey(dossierFileId);

		Date now = new Date();

		dossierFile.setModifiedDate(now);

		dossierFile.setDisplayName(displayName);

		dossierFile.setFileEntryId(fileEntryId);
		dossierFile.setOwnerUserId(ownerUserId);
		dossierFile.setOwnerOrganizationId(ownerOrganizationId);
		dossierFile = dossierFilePersistence.update(dossierFile);
		Indexer indexer = IndexerRegistryUtil
				.nullSafeGetIndexer(DossierFile.class);

		indexer.reindex(dossierFile);

		return dossierFile;
	}

	/**
	 * @param dossierFileId
	 * @param userId
	 * @param dossierId
	 * @param dossierPartId
	 * @param templateFileNo
	 * @param groupFileId
	 * @param ownerUserId
	 * @param ownerOrganizationId
	 * @param displayName
	 * @param formData
	 * @param fileEntryId
	 * @param dossierFileMark
	 * @param dossierFileType
	 * @param dossierFileNo
	 * @param dossierFileDate
	 * @param original
	 * @param syncStatus
	 * @param serviceContext
	 * @return
	 * @throws NoSuchDossierFileException
	 * @throws SystemException
	 */
	public DossierFile updateDossierFile(long dossierFileId, long userId,
			long dossierId, long dossierPartId, String templateFileNo,
			long groupFileId, long ownerUserId, long ownerOrganizationId,
			String displayName, String formData, long fileEntryId,
			int dossierFileMark, int dossierFileType, String dossierFileNo,
			Date dossierFileDate, int original, int syncStatus,
			ServiceContext serviceContext) throws NoSuchDossierFileException,
			SystemException {

		DossierFile dossierFile = dossierFilePersistence
				.findByPrimaryKey(dossierFileId);

		Date now = new Date();

		dossierFile.setModifiedDate(now);
		dossierFile.setUserId(userId);
		dossierFile.setDisplayName(displayName);
		dossierFile.setDossierFileDate(dossierFileDate);
		dossierFile.setDossierFileMark(dossierFileMark);
		dossierFile.setDossierFileNo(dossierFileNo);
		dossierFile.setDossierFileType(dossierFileType);
		dossierFile.setDossierId(dossierId);
		dossierFile.setDossierPartId(dossierPartId);
		dossierFile.setFileEntryId(fileEntryId);
		dossierFile.setFormData(formData);
		dossierFile.setGroupFileId(groupFileId);
		dossierFile.setOriginal(original);
		dossierFile.setOwnerOrganizationId(ownerOrganizationId);
		return dossierFilePersistence.update(dossierFile);
	}

	/**
	 * @param dossierFileId
	 * @param folderId
	 * @param sourceFileName
	 * @param mimeType
	 * @param title
	 * @param description
	 * @param changeLog
	 * @param is
	 * @param size
	 * @param serviceContext
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	public DossierFile updateDossierFile(long dossierFileId, long folderId,
			String sourceFileName, String mimeType, String title,
			String description, String changeLog, InputStream is, long size,
			ServiceContext serviceContext) throws SystemException,
			PortalException {

		DossierFile dossierFile = dossierFilePersistence
				.findByPrimaryKey(dossierFileId);

		Date now = new Date();

		dossierFile.setModifiedDate(now);
		// Add file
		FileEntry fileEntry = dlAppService.addFileEntry(
				serviceContext.getScopeGroupId(), folderId, sourceFileName,
				mimeType, getFileName(sourceFileName), description, changeLog,
				is, size, serviceContext);

		dossierFile.setFileEntryId(fileEntry.getFileEntryId());
		return dossierFilePersistence.update(dossierFile);
	}

	/**
	 * @param userId
	 * @param dossierId
	 * @param syncStatus
	 * @param worklows
	 * @throws SystemException
	 * @throws NoSuchDossierStatusException
	 * @throws PortalException
	 */
	public List<DossierFile> updateDossierFileResultSyncStatus(long userId,
			long dossierId, int currentSyncStatus, int syncStatus, int removed,
			List<WorkflowOutput> workflowOutputs) throws SystemException,
			NoSuchDossierStatusException, PortalException {

		List<DossierFile> dossierFiles = new ArrayList<DossierFile>();
		Date now = new Date();
		try {
			if (workflowOutputs != null) {
				for (WorkflowOutput workflowOutput : workflowOutputs) {

					List<DossierFile> dossierFileTemps = dossierFileLocalService
							.getDossierFileByDID_SS_DPID_R(dossierId,
									currentSyncStatus,
									workflowOutput.getDossierPartId(), removed);

					if (dossierFileTemps != null) {
						for (DossierFile dossierFile : dossierFileTemps) {
							dossierFile.setSyncStatus(syncStatus);
							dossierFile.setModifiedDate(now);
							if (userId != 0) {
								dossierFile.setUserId(userId);
							}
							dossierFileLocalService
									.updateDossierFile(dossierFile);
							dossierFiles.add(dossierFile);
						}
					}

				}
			}

		}

		catch (Exception e) {
			_log.info("//////////////////////////////////////////////////NO FILE RESULT UPLOAD");
		}

		return dossierFiles;
	}

	/**
	 * @param userId
	 * @param syncStatus
	 * @param dossierFiles
	 * @return
	 * @throws SystemException
	 * @throws NoSuchDossierStatusException
	 * @throws PortalException
	 */
	public List<DossierFile> updateDossierFileSyncStatus(long userId,
			int syncStatus, List<DossierFile> dossierFiles)
			throws SystemException, NoSuchDossierStatusException,
			PortalException {

		System.out
				.println("DOSSSSSSSSSSSSSIERRRRRRRRRRR" + dossierFiles.size());

		Date now = new Date();
		try {
			if (dossierFiles != null) {
				for (DossierFile dossierFile : dossierFiles) {

					dossierFile.setSyncStatus(syncStatus);
					dossierFile.setModifiedDate(now);
					if (userId != 0) {
						dossierFile.setUserId(userId);
					}
					dossierFileLocalService.updateDossierFile(dossierFile);
				}
			}

		}

		catch (Exception e) {
			_log.info("//////////////////////////////////////////////////NO FILE RESULT UPLOAD");
		}

		return dossierFiles;
	}

	/**
	 * @param userId
	 * @param dossierId
	 * @param curSyncStatus
	 * @param newSyncStatus
	 * @param removed
	 * @throws SystemException
	 * @throws NoSuchDossierStatusException
	 * @throws PortalException
	 */
	public void updateDossierFileSyncStatus(long userId, long dossierId,
			int curSyncStatus, int newSyncStatus, int removed)
			throws SystemException, NoSuchDossierStatusException,
			PortalException {

		Date now = new Date();

		List<DossierFile> dossierFiles = dossierFileLocalService
				.getDossierFileByDID_SS_R(dossierId, curSyncStatus, removed);

		if (dossierFiles != null) {
			for (DossierFile dossierFile : dossierFiles) {
				if (dossierFile.getDossierFileType() != PortletConstants.DOSSIER_FILE_TYPE_OUTPUT) {
					dossierFile.setSyncStatus(newSyncStatus);
					dossierFile.setModifiedDate(now);
					if (userId != 0) {
						dossierFile.setUserId(userId);
					}
					dossierFileLocalService.updateDossierFile(dossierFile);
				}

			}
		}
	}

	/**
	 * @param userId
	 * @param dossierId
	 * @param syncStatus
	 * @throws SystemException
	 * @throws NoSuchDossierStatusException
	 * @throws PortalException
	 */
	public void updateDossierFileSyncStatus(long userId, long fileGroupId,
			long dossierId, int curSyncStatus, int newSyncStatus, int removed)
			throws SystemException, NoSuchDossierStatusException,
			PortalException {

		Date now = new Date();

		List<DossierFile> dossierFiles = dossierFileLocalService
				.getDossierFileByDID_GFID_SS_R(dossierId, fileGroupId,
						curSyncStatus, removed);

		if (dossierFiles != null) {
			for (DossierFile dossierFile : dossierFiles) {
				if (dossierFile.getDossierFileType() != PortletConstants.DOSSIER_FILE_TYPE_OUTPUT) {
					dossierFile.setSyncStatus(newSyncStatus);
					dossierFile.setModifiedDate(now);
					if (userId != 0) {
						dossierFile.setUserId(userId);
					}
					dossierFileLocalService.updateDossierFile(dossierFile);
				}
			}
		}
	}

	/**
	 * @param dossierId
	 * @param templateFileNo
	 * @return
	 * @throws SystemException
	 */
	public DossierFile fetchByTemplateFileNoDossierId_First(long dossierId,
			String templateFileNo) throws SystemException {

		OrderByComparatorFactory orderByComparatorFactory = OrderByComparatorFactoryUtil
				.getOrderByComparatorFactory();
		OrderByComparator comparator = orderByComparatorFactory.create(
				"DossierFile", "modifiedDate", true);
		return dossierFilePersistence.fetchByTemplateFileNoDossierId_First(
				dossierId, templateFileNo, comparator);
	}

	/**
	 * @param oid
	 * @return
	 * @throws SystemException
	 */
	public DossierFile getByOid(String oid) throws SystemException {

		return dossierFilePersistence.fetchByOid(oid);
	}

	/**
	 * Ham chuyen ten file nguoi dung upload len thanh ten do he thong quy dinh
	 * 
	 * @param sourceFileName
	 * @return
	 */
	private String getFileName(String sourceFileName) {

		String ext = FileUtil.getExtension(sourceFileName);

		StringBuilder sbFileName = new StringBuilder(5);

		sbFileName.append(PortalUUIDUtil.generate());
		sbFileName.append(StringPool.DASH);
		sbFileName.append(System.nanoTime());

		if (Validator.isNotNull(ext)) {
			sbFileName.append(StringPool.PERIOD);
			sbFileName.append(ext);
		}

		return sbFileName.toString();
	}

	/**
	 * Ham lay ra cac giay to loai nhieu ket qua hoac giay to ket qua
	 * 
	 * @param dossierId
	 * @param orderByComparator
	 * @return
	 */

	public List<DossierFile> getDossierFileResult(long dossierId, int start,
			int end, OrderByComparator orderByComparator)
			throws SystemException {

		return dossierFileFinder.searchDossierFileResult(0, dossierId, 2, 5, 6,
				start, end, orderByComparator);
	}
	
	public List<DossierFile> getDossierFileSuggesstion(
			long dossierId, List<String> templateFileNos, long userId, int start, int end) 
			throws SystemException {

		String templateFileNosStr = StringUtil.merge(templateFileNos, "','");
		
		return dossierFileFinder.searchDossierFileSuggestion(dossierId, templateFileNosStr, userId, start, end);
	}

	public DossierFile getLastestDossierFile() throws SystemException {
		List<DossierFile> dossierFiles = dossierFilePersistence.findAll(0, 1);

		return dossierFiles.get(0);
	}

	private static Log _log = LogFactoryUtil
			.getLog(DossierFileLocalServiceImpl.class.getName());

}
