package org.meveo.commons.utils;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tyshan　Shi(tyshan@manaty.net)
 **/
public class XStreamCDATAConverter implements Converter {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String CDATA_START = "<![CDATA[";
    public static final String CDATA_END = "]]>";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class clazz) {
        return true;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        log.debug("start marshall...");
        if (object == null) {
            writer.setValue("");
        } else {
            writer.setValue(CDATA_START + object + CDATA_END);
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        throw new ConversionException("Not support to unmarshall");
    }

}
