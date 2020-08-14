package org.meveo.api.notification;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.BaseCrudApi;
import org.meveo.api.dto.notification.JobTriggerDto;
import org.meveo.api.exception.EntityAlreadyExistsException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.exception.InvalidParameterException;
import org.meveo.api.exception.MeveoApiException;
import org.meveo.api.exception.MissingParameterException;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.catalog.CounterTemplate;
import org.meveo.model.jobs.JobInstance;
import org.meveo.model.notification.JobTrigger;
import org.meveo.model.scripts.ScriptInstance;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.CounterTemplateService;
import org.meveo.service.job.JobInstanceService;
import org.meveo.service.notification.JobTriggerService;
import org.meveo.service.script.ScriptInstanceService;

/**
 * @author Tyshan Shi
 * @author Edward P. Legaspi | edward.legaspi@manaty.net
 * @version 6.10
 **/
@Stateless
public class JobTriggerApi extends BaseCrudApi<JobTrigger, JobTriggerDto> {

    public JobTriggerApi() {
		super(JobTrigger.class, JobTriggerDto.class);
	}

	@Inject
    private JobTriggerService jobTriggerService;

    @Inject
    private CounterTemplateService counterTemplateService;

    @Inject
    private ScriptInstanceService scriptInstanceService;

    @Inject
    private JobInstanceService jobInstanceService;

    public JobTrigger create(JobTriggerDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getClassNameFilter())) {
            missingParameters.add("classNameFilter");
        }
        if (postData.getEventTypeFilter() == null) {
            missingParameters.add("eventTypeFilter");
        }
        handleMissingParameters();

        if (jobTriggerService.findByCode(postData.getCode()) != null) {
            throw new EntityAlreadyExistsException(JobTrigger.class, postData.getCode());
        }
        
        JobTrigger notif = fromDto(postData);
        
        jobTriggerService.create(notif);

        return notif;
    }

    /* (non-Javadoc)
     * @see org.meveo.api.ApiService#find(java.lang.String)
     */
    @Override
    public JobTriggerDto find(String notificationCode) throws EntityDoesNotExistsException, MissingParameterException, InvalidParameterException, MeveoApiException {
        JobTriggerDto result = new JobTriggerDto();

        if (!StringUtils.isBlank(notificationCode)) {
            JobTrigger notif = jobTriggerService.findByCode(notificationCode);

            if (notif == null) {
                throw new EntityDoesNotExistsException(JobTrigger.class, notificationCode);
            }

            result = new JobTriggerDto(notif);
        } else {
            missingParameters.add("code");

            handleMissingParameters();
        }

        return result;
    }
        
    public JobTrigger update(JobTriggerDto postData) throws MeveoApiException, BusinessException {

        if (StringUtils.isBlank(postData.getCode())) {
            missingParameters.add("code");
        }
        if (StringUtils.isBlank(postData.getClassNameFilter())) {
            missingParameters.add("classNameFilter");
        }
        if (postData.getEventTypeFilter() == null) {
            missingParameters.add("eventTypeFilter");
        }
        handleMissingParameters();

        JobTrigger notif = jobTriggerService.findByCode(postData.getCode());
        if (notif == null) {
            throw new EntityDoesNotExistsException(JobTrigger.class, postData.getCode());
        }
        return update(postData, notif);
    }

	public JobTrigger update(JobTriggerDto postData, JobTrigger notif) throws EntityDoesNotExistsException, InvalidParameterException, BusinessException {
		ScriptInstance scriptInstance = null;
        if (!StringUtils.isBlank(postData.getScriptInstanceCode())) {
            scriptInstance = scriptInstanceService.findByCode(postData.getScriptInstanceCode());
            if (scriptInstance == null) {
                throw new EntityDoesNotExistsException(ScriptInstance.class, postData.getScriptInstanceCode());
            }
        }
        // check class
        try {
            Class.forName(postData.getClassNameFilter());
        } catch (Exception e) {
            throw new InvalidParameterException("classNameFilter", postData.getClassNameFilter());
        }

        CounterTemplate counterTemplate = null;
        if (!StringUtils.isBlank(postData.getCounterTemplate())) {
            counterTemplate = counterTemplateService.findByCode(postData.getCounterTemplate());
            if (counterTemplate == null) {
                throw new EntityDoesNotExistsException(CounterTemplate.class, postData.getCounterTemplate());
            }
        }

        JobInstance jobInstance = null;
        if (!StringUtils.isBlank(postData.getJobInstance())) {
            jobInstance = jobInstanceService.findByCode(postData.getJobInstance());
            if (jobInstance == null) {
                throw new EntityDoesNotExistsException(JobInstance.class, postData.getJobInstance());
            }
        }

        notif.setCode(StringUtils.isBlank(postData.getUpdatedCode()) ? postData.getCode() : postData.getUpdatedCode());
        notif.setClassNameFilter(postData.getClassNameFilter());
        notif.setEventTypeFilter(postData.getEventTypeFilter());
        notif.setFunction(scriptInstance);
        notif.setParams(postData.getScriptParams());
        notif.setElFilter(postData.getElFilter());
        notif.setCounterTemplate(counterTemplate);
        notif.setJobInstance(jobInstance);
        notif.setJobParams(postData.getJobParams());

        notif = jobTriggerService.update(notif);

        return notif;
	}

    public void remove(String notificationCode) throws MeveoApiException, BusinessException {
        if (!StringUtils.isBlank(notificationCode)) {
            JobTrigger notif = jobTriggerService.findByCode(notificationCode);

            if (notif == null) {
                throw new EntityDoesNotExistsException(JobTrigger.class, notificationCode);
            }

            jobTriggerService.remove(notif);
        } else {
            missingParameters.add("code");

            handleMissingParameters();
        }
    }

    @Override
    public JobTrigger createOrUpdate(JobTriggerDto postData) throws MeveoApiException, BusinessException {
        if (jobTriggerService.findByCode(postData.getCode()) == null) {
            return create(postData);
        } else {
            return update(postData);
        }
    }

	@Override
	public JobTriggerDto toDto(JobTrigger entity) {
		return new JobTriggerDto(entity);
	}

	@Override
	public JobTrigger fromDto(JobTriggerDto postData) throws MeveoApiException {
        ScriptInstance scriptInstance = null;
        if (!StringUtils.isBlank(postData.getScriptInstanceCode())) {
            scriptInstance = scriptInstanceService.findByCode(postData.getScriptInstanceCode());
            if (scriptInstance == null) {
                throw new EntityDoesNotExistsException(ScriptInstance.class, postData.getScriptInstanceCode());
            }
        }
        // check class
        try {
            Class.forName(postData.getClassNameFilter());
        } catch (Exception e) {
            throw new InvalidParameterException("classNameFilter", postData.getClassNameFilter());
        }

        CounterTemplate counterTemplate = null;
        if (!StringUtils.isBlank(postData.getCounterTemplate())) {
            counterTemplate = counterTemplateService.findByCode(postData.getCounterTemplate());
            if (counterTemplate == null) {
                throw new EntityDoesNotExistsException(CounterTemplate.class, postData.getCounterTemplate());
            }
        }
        
        JobInstance jobInstance = null;
        if (!StringUtils.isBlank(postData.getJobInstance())) {
            jobInstance = jobInstanceService.findByCode(postData.getJobInstance());
            if (jobInstance == null) {
                throw new EntityDoesNotExistsException(JobInstance.class, postData.getJobInstance());
            }
        }

        JobTrigger notif = new JobTrigger();
        notif.setCode(postData.getCode());
        notif.setClassNameFilter(postData.getClassNameFilter());
        notif.setEventTypeFilter(postData.getEventTypeFilter());
        notif.setFunction(scriptInstance);
        notif.setParams(postData.getScriptParams());
        notif.setElFilter(postData.getElFilter());
        notif.setCounterTemplate(counterTemplate);
        notif.setJobInstance(jobInstance);
        notif.setJobParams(postData.getJobParams());
        
		return notif;
	}

	@Override
	public IPersistenceService<JobTrigger> getPersistenceService() {
		return jobTriggerService;
	}

	@Override
	public boolean exists(JobTriggerDto dto) {
		return jobTriggerService.findByCode(dto.getCode()) != null;
	}

	@Override
	public void remove(JobTriggerDto dto) throws MeveoApiException, BusinessException {
		var jobTrigger = jobTriggerService.findByCode(dto.getCode());
		if(jobTrigger != null) {
			jobTriggerService.remove(jobTrigger);
		}
	}
}
