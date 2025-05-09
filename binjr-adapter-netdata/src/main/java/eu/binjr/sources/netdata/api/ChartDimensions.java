/*
 * NetData API
 * Real-time performance and health monitoring.
 *
 * The version of the OpenAPI document: 1.11.1_rolling
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package eu.binjr.sources.netdata.api;

import com.google.gson.annotations.SerializedName;


import java.util.Objects;

/**
 * ChartDimensions
 */
public class ChartDimensions {
    public static final String SERIALIZED_NAME_NAME = "name";
    @SerializedName(SERIALIZED_NAME_NAME)
    private String name;


    public ChartDimensions name(String name) {

        this.name = name;
        return this;
    }

    /**
     * The name of the dimension
     *
     * @return name
     **/
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChartDimensions chartDimensions = (ChartDimensions) o;
        return Objects.equals(this.name, chartDimensions.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }


    @Override
    public String toString() {
        return "class ChartDimensions {\n" +
                "    name: " + toIndentedString(name) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}

