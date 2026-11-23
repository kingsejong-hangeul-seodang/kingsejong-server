package com.kingsejong.global.validation;

import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;

import java.util.List;

/**
 * 원고지 칸 규칙. 교정 엔진(app/schemas.py GradeRequest)과 같은 규칙을 서버가 먼저 검사한다
 * (엔진에서 걸리면 502가 되므로 서버에서 422로 막는다). 쓰기 제출과 레벨 테스트가 같이 쓴다.
 * - index는 0부터 빠짐없이 순서대로
 * - 초보·중수: 칸 수 = 정답에서 공백을 뺀 글자 수 / 고수: 칸 수 ≥ 정답 칸 수(띄어쓰기 포함)
 */
public final class CellRules {

    private CellRules() {
    }

    public static void check(Stage stage, String target, List<Integer> indexes) {
        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i) != i) {
                throw invalid("cells[" + i + "].index", "index는 0부터 빠짐없이 순서대로 와야 해요");
            }
        }
        if (stage == Stage.ADVANCED) {
            if (indexes.size() < target.length()) {
                throw invalid("cells", "칸이 " + target.length() + "개 이상이어야 해요");
            }
        } else {
            int need = target.replace(" ", "").length();
            if (indexes.size() != need) {
                throw invalid("cells", "칸이 " + need + "개여야 해요");
            }
        }
    }

    private static BusinessException invalid(String field, String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail(field, message)));
    }
}
