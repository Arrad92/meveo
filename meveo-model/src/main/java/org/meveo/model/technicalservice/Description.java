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
package org.meveo.model.technicalservice;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe the input and output properties for a variable
 *
 * @author Clément Bareth
 */
@GenericGenerator(
        name = "ID_GENERATOR",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {@Parameter(name = "sequence_name", value = "technical_services_description_seq")}
)
@Entity
@Table(name = "technical_services_description")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "description_type")
public abstract class Description {

    @Id
    @GeneratedValue(generator = "ID_GENERATOR", strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private TechnicalService service;

    /**
     * List of properties that are defined as inputs. Non empty list implies input = true.
     */
    @OneToMany(mappedBy = "description", targetEntity = PropertyDescription.class)
    private List<InputProperty> inputProperties = new ArrayList<>();

    /**
     * List of properties that are defined as outputs. Non empty list implies output = true.
     */
    @OneToMany(mappedBy = "description", targetEntity = PropertyDescription.class)
    private List<OutputProperty> outputProperties = new ArrayList<>();

    /**
     * Whether the variable is defined as input of the connector.
     */
    private boolean input;

    /**
     * Whether the variable is defined as output of the connector.
     */
    private boolean output;

    /**
     * List of properties that are defined as inputs. Non empty list implies input = true.
     * @return The list of the properties that are defined as inputs.
     */
    public List<InputProperty> getInputProperties() {
        return inputProperties;
    }

    /**
     * List of properties that are defined as outputs. Non empty list implies output = true.
     * @return The list of the properties that are defined as inputs.
     */
    public List<OutputProperty> getOutputProperties() {
        return outputProperties;
    }

    /**
     * Whether the variable is defined as input of the connector.
     * @return "true" if the variable is an input.
     */
    public boolean isOutput() {
        return output;
    }

    /**
     * Whether the variable is defined as output of the connector.
     * @return "false" if the variable is an input.
     */
    public boolean isInput(){
        return input;
    }

    /**
     * Name of the variable described
     * @return The name of the variable
     */
    public abstract String getName();

    public abstract String getTypeName();

    public void setInputProperties(List<InputProperty> inputProperties) {
        this.inputProperties = inputProperties;
    }

    public void setOutputProperties(List<OutputProperty> outputProperties) {
        this.outputProperties = outputProperties;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public void setOutput(boolean output) {
        this.output = output;
    }

    public TechnicalService getService() {
        return service;
    }

    public void setService(TechnicalService service) {
        this.service = service;
    }

    public long getId() {
        return id;
    }

}
