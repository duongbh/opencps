<%
/**
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
 */
%>

<%@ include file="../init.jsp"%>

<%
	String tab1 = ParamUtil.getString(request, "tab1", "upload-file");
%>

<liferay-ui:tabs
	names="upload-file,select-file-store"
	refresh="<%= false %>"
	value="<%=tab1 %>"
>
	<liferay-ui:section>
		<liferay-util:include 
			page="/html/common/portlet/dossier_upload_file.jsp" 
			servletContext="<%=application %>"
		>
			<portlet:param name="offsetX" value="<%=String.valueOf(offsetX) %>"/>
			<portlet:param name="offsetY" value="<%=String.valueOf(offsetY) %>"/>
			<portlet:param name="signatureType" value="<%=signatureType %>"/>
			<portlet:param name="characterAttachs" value="<%=StringUtil.merge(characterAttachs) %>"/>
			<portlet:param name="textPositionWithImageSign" value="<%=textPositionWithImageSign %>"/>
		</liferay-util:include>
	</liferay-ui:section>
	<liferay-ui:section >
		
		<liferay-util:include 
			page='<%=templatePath + "dossier_select_file.jsp" %>' 
			servletContext="<%=application %>"
		/>
	</liferay-ui:section>
</liferay-ui:tabs>