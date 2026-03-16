package org.miowing.mioverify.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <h1>用户信息</h1>
 * 序列化后的 Json 格式，不对应数据库
 * <pre>
 * {
 * 	"id":"用户的 ID",
 * 	"properties":[ // 用户的属性（数组，每一元素为一个属性）
 *                { // 一项属性
 * 			"name":"属性的名称",
 * 			"value":"属性的值",
 *        }
 * 		// ,...（可以有更多）
 * 	]
 * }
 * </pre>
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserShow {
    private String id;
    private List<Property> properties;
    @Data
    @Accessors(chain = true)
    public static class Property {
        private String name;
        private String value;
    }
}