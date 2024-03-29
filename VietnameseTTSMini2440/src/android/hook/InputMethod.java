package android.hook;


/**
 * interface dùng để định nghĩa các kiểu gõ, dùng cho các lớp quy định các kiểu gõ kế thừa
 * 
 * @author LamPT
 *
 */
public interface InputMethod {
	
	/** 
	 * hàm chuyển kí tự dấu tương ứng từ kiểu gõ Auto về kiểu gõ VNI
	 * 
	 * @keyChar kí tự nhập vào
	 * @curChar kí tự tại vị trí hiện tại của con trỏ
	 * @curWord từ xác định tại vị trí hiện tại của con trỏ
	 * @return trả về kí tự dấu tương ứng 
	 */
	public char getAccentMark(char keyChar, char curChar, String curWord);

}
