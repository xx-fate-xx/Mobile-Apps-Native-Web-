import SwiftUI
import Shared

// Task 14 - iOS side, the same LoginValidator from commonMain
class LoginViewModel: ObservableObject {
    private let validator = LoginValidator()

    @Published var email = ""
    @Published var password = ""
    @Published var error: String? = nil

    func onLoginClick() {
        error = validator.validate(email: email, password: password)
    }
}

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()

    var body: some View {
        VStack {
            TextField("Email", text: $viewModel.email)
            SecureField("Password", text: $viewModel.password)
            if let error = viewModel.error {
                Text(error).foregroundColor(.red)
            }
            Button("Login") {
                viewModel.onLoginClick()
            }
        }
        .padding()
    }
}
