/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.api.rest.module.impl;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.ActionStatusEnum;
import org.meveo.api.dto.module.MeveoModuleDto;
import org.meveo.api.dto.response.module.MeveoModuleDtoResponse;
import org.meveo.api.dto.response.module.MeveoModuleDtosResponse;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.logging.WsRestApiInterceptor;
import org.meveo.api.module.MeveoModuleApi;
import org.meveo.api.rest.impl.BaseRs;
import org.meveo.api.rest.module.ModuleRs;
import org.meveo.model.module.MeveoModule;
import org.meveo.service.admin.impl.MeveoModuleFilters;

/**
 * @author Clément Bareth
 * @author Tyshan Shi(tyshan@manaty.net)
 * @lastModifiedVersion 6.3.0
 */
@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class ModuleRsImpl extends BaseRs implements ModuleRs {

    @Inject
    private MeveoModuleApi moduleApi;

    @Override
    public ActionStatus create(MeveoModuleDto moduleData, boolean development) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            moduleApi.create(moduleData, development);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus update(MeveoModuleDto moduleDto) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            moduleApi.update(moduleDto);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus delete(String code) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            moduleApi.delete(code);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public Response list(boolean codesOnly, MeveoModuleFilters filters) {

        if(!codesOnly) {
            MeveoModuleDtosResponse result = new MeveoModuleDtosResponse();
            result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);
            result.getActionStatus().setMessage("");
            try {
                List<MeveoModuleDto> dtos = moduleApi.list(filters);
                result.setModules(dtos);
            } catch (Exception e) {
                processException(e, result.getActionStatus());
            }

            return Response.ok(result).build();
        } else {
            return Response.ok(moduleApi.listCodesOnly(filters)).build();
        }

    }

    @Override
    public MeveoModuleDtoResponse get(String code) {
        MeveoModuleDtoResponse result = new MeveoModuleDtoResponse();
        result.getActionStatus().setStatus(ActionStatusEnum.SUCCESS);

        try {
            result.setModule(moduleApi.find(code));
        } catch (Exception e) {
            processException(e, result.getActionStatus());
        }

        return result;
    }

    @Override
    public ActionStatus createOrUpdate(MeveoModuleDto moduleDto) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            moduleApi.createOrUpdate(moduleDto);
        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus install(MeveoModuleDto moduleDto) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            moduleApi.install(moduleDto);

        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus uninstall(String code, boolean remove) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            moduleApi.uninstall(code, MeveoModule.class, remove);

        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus enable(String code) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            moduleApi.enable(code, MeveoModule.class);

        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public ActionStatus disable(String code) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");

        try {
            moduleApi.disable(code, MeveoModule.class);

        } catch (Exception e) {
            processException(e, result);
        }

        return result;
    }

    @Override
    public MeveoModuleDto addToModule(String moduleCode, String itemCode, String itemType) throws EntityDoesNotExistsException, BusinessException {
        return moduleApi.addToModule(moduleCode, itemCode, itemType);
    }

    @Override
    public MeveoModuleDto removeFromModule(String moduleCode, String itemCode, String itemType) throws EntityDoesNotExistsException, BusinessException {
        return moduleApi.removeFromModule(moduleCode, itemCode, itemType);
    }
}
