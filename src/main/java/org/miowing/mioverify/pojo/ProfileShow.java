package org.miowing.mioverify.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.lang.Nullable;

import java.util.List;

/**
 * <h1>角色信息</h1>
 * 序列化后的 Json 格式，不对应数据库
 * <pre>
 * {
 * 	"id":"角色 UUID（无符号）",
 * 	"name":"角色名称",
 * 	"properties":[ // 角色的属性（数组，每一元素为一个属性）（仅在特定情况下需要包含）
 *                { // 一项属性
 * 			"name":"属性的名称",
 * 			"value":"属性的值",
 * 			"signature":"属性值的数字签名（仅在特定情况下需要包含）"
 *        }
 * 		// ,...（可以有更多）
 * 	]
 * }
 * </pre>
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProfileShow {
    private String id;
    private String name;
    private List<Property> properties;
    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Property {
        private String name;
        private String value;
        private @Nullable String signature;
    }
}