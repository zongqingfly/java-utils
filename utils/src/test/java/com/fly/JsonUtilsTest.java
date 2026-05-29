package com.fly;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonUtils 测试用例
 */
class JsonUtilsTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outputStreamCaptor;

    @BeforeEach
    void setUp() {
        outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ==================== toJson 测试 ====================

    @Test
    void toJson_shouldConvertSimpleObjectObj2Json() {
        // given
        Person person = new Person("张三", 30);

        // when
        String json = JsonUtils.obj2Json(person);

        // then
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":30"));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void toJson_shouldConvertMapObj2Json() {
        // given
        Map<String, Object> map = Map.of("key1", "value1", "key2", 123);

        // when
        String json = JsonUtils.obj2Json(map);

        // then
        assertTrue(json.contains("\"key1\":\"value1\""));
        assertTrue(json.contains("\"key2\":123"));
    }

    @Test
    void toJson_shouldConvertListObj2Json() {
        // given
        List<String> list = List.of("a", "b", "c");

        // when
        String json = JsonUtils.obj2Json(list);

        // then
        assertEquals("[\"a\",\"b\",\"c\"]", json);
    }

    @Test
    void toJson_whenNullObject_shouldConvertObj2NullJson() {
        // when
        String json = JsonUtils.obj2Json(null);

        // then
        assertEquals("null", json);
    }

    // ==================== fromJson 测试 ====================

    @Test
    void json2ObjToSimpleObject() {
        // given
        String json = "{\"name\":\"李四\",\"age\":25}";

        // when
        Person person = JsonUtils.json2Obj(json, Person.class);

        // then
        assertNotNull(person);
        assertEquals("李四", person.getName());
        assertEquals(25, person.getAge());
    }

    @Test
    void json2ObjToList() {
        // given
        String json = "[\"apple\",\"banana\",\"cherry\"]";

        // when
        List<String> list = JsonUtils.json2Obj(json, List.class);

        // then
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("apple", list.get(0));
        assertEquals("banana", list.get(1));
        assertEquals("cherry", list.get(2));
    }

    @Test
    void json2Obj_shouldThrowRuntimeException() {
        // given
        String invalidJson = "{name:}";

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> JsonUtils.json2Obj(invalidJson, Person.class));

        assertEquals("Error converting JSON to object", exception.getMessage());
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof JsonProcessingException);
    }

    @Test
    void json2Obj_whenEmptyString_shouldThrowException() {
        assertThrows(RuntimeException.class,
                () -> JsonUtils.json2Obj("", Person.class));
    }

    @Test
    void json2Obj_shouldThrowException() {
        assertThrows(RuntimeException.class,
                () -> JsonUtils.json2Obj(null, Person.class));
    }

    // ==================== printJson 测试 ====================

    @Test
    void printJson_shouldPrintObjByJsonToConsole() {
        // given
        Person person = new Person("王五", 40);

        // when
        JsonUtils.printObjByJson(person);

        // then
        String printedOutput = outputStreamCaptor.toString().trim();
        assertTrue(printedOutput.contains("\"name\":\"王五\""));
        assertTrue(printedOutput.contains("\"age\":40"));
        assertTrue(printedOutput.startsWith("{"));
        assertTrue(printedOutput.endsWith("}"));
    }

    @Test
    void printJson_whenNullObject_shouldPrintObjByNull() {
        // when
        JsonUtils.printObjByJson(null);

        // then
        String printedOutput = outputStreamCaptor.toString().trim();
        assertEquals("null", printedOutput);
    }

    @Test
    void printJson_shouldPrintObjByEachCallOnNewLine() {
        // given
        Person person1 = new Person("赵六", 20);
        Person person2 = new Person("孙七", 30);

        // when
        JsonUtils.printObjByJson(person1);
        JsonUtils.printObjByJson(person2);

        // then
        String printedOutput = outputStreamCaptor.toString().trim();
        String[] lines = printedOutput.split(System.lineSeparator());

        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("赵六"));
        assertTrue(lines[1].contains("孙七"));
    }

    // ==================== 辅助测试类 ====================

    static class Person {
        private String name;
        private int age;

        // 无参构造（Jackson 反序列化需要）
        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}