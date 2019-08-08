/*
 * (C) Copyright 2015-2016 Opencell SAS (http://opencellsoft.com/) and contributors.
 * (C) Copyright 2009-2014 Manaty SARL (http://manaty.net/) and contributors.
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
package org.meveo.admin.action.admin.module;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.crm.CustomFieldTemplate;
import org.meveo.model.module.MeveoModule;
import org.meveo.service.base.local.IPersistenceService;

import java.util.List;

/**
 * Meveo module bean
 *
 * @author Tyshan Shi(tyshan@manaty.net)
 *
 */

@Named
@ViewScoped
public class MeveoModuleBean extends GenericModuleBean<MeveoModule> {

    private static final long serialVersionUID = 1L;

    private String moduleCode;
    private List<MeveoModule> meveoModules;


    /**
     * Constructor. Invokes super constructor and provides class type of this bean for {@link BaseBean}.
     */
    public MeveoModuleBean() {
        super(MeveoModule.class);
    }

    /**
     * @see BaseBean#getPersistenceService()
     */
    @Override
    protected IPersistenceService<MeveoModule> getPersistenceService() {
        return meveoModuleService;
    }

    public void initializeModules() {
        meveoModules = meveoModuleService.findLikeWithCode(moduleCode);
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public List<MeveoModule> getMeveoModules() {
        return meveoModules;
    }

    public void setMeveoModules(List<MeveoModule> meveoModules) {
        this.meveoModules = meveoModules;
    }

    public void searchModules() {
        meveoModules = meveoModuleService.findLikeWithCode(moduleCode);
    }
}