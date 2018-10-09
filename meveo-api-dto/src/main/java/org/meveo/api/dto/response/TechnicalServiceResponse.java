/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
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
package org.meveo.api.dto.response;

import org.meveo.api.dto.technicalservice.TechnicalServiceDto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Clément Bareth
 */
@XmlRootElement(name = "TechnicalServiceResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class TechnicalServiceResponse extends BaseResponse {

    private static final long serialVersionUID = 1507326300507935339L;

    private TechnicalServiceDto technicalServiceDto;

    /**
     * Data representing the TechnicalService queried
     *
     * @return DTO object
     */
    public TechnicalServiceDto getTechnicalServiceDto() {
        return technicalServiceDto;
    }

    /**
     * Data representing the TechnicalService queried
     *
     * @return DTO object
     */
    public void setTechnicalServiceDto(TechnicalServiceDto connectorDto) {
        this.technicalServiceDto = connectorDto;
    }

    @Override
    public String toString() {
        return "TechnicalServiceResponse [technicalservice=" + technicalServiceDto + ", toString()=" + super.toString() + "]";
    }

}
